package com.xypu.controller;

import com.alibaba.fastjson.JSON;
import com.wf.captcha.ArithmeticCaptcha;
import com.xypu.context.UserContext;
import com.xypu.entity.dto.LoginDTO;
import com.xypu.entity.dto.RegisterDTO;
import com.xypu.entity.vo.UserInfoVO;
import com.xypu.enums.PermissionEnum;
import com.xypu.exception.BusinessException;
import com.xypu.exception.ErrorCodeEnum;
import com.xypu.response.ResponseVO;
import com.xypu.service.UserInfoService;
import com.xypu.utils.RedisUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/client")
public class UserAuthController {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private UserInfoService userInfoService;

    /**
     * 获取图形验证码
     * 验证码答案存入 Redis，Key 由前端持有，TTL 5 分钟
     */
    @GetMapping("/checkCode")
    public ResponseVO<Map<String, String>> checkCode() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 48);
        String answer = captcha.text();
        String base64 = captcha.toBase64();
        // 生成唯一 key，与答案绑定存入 Redis
        String key = UUID.randomUUID().toString().replace("-", "");
        redisUtils.set(RedisUtils.CHECK_CODE_CLIENT_PREFIX + key, answer, RedisUtils.CHECK_CODE_TTL);
        Map<String, String> data = new HashMap<>();
        data.put("checkCodeKey", key);
        data.put("checkCodeBase64", base64);
        return ResponseVO.ok(data);
    }

    /**
     * 用户注册
     * 校验验证码后调用 Service 完成注册，默认权限为普通用户
     */
    @PostMapping("/register")
    public ResponseVO<Void> register(@RequestBody RegisterDTO dto) {
        // 先校验验证码，通过后自动从 Redis 删除（一次性使用）
        verifyCheckCode(RedisUtils.CHECK_CODE_CLIENT_PREFIX + dto.getCheckCodeKey(), dto.getCheckCode());
        userInfoService.register(dto.getAccount(), dto.getPassword(), dto.getNickName(), PermissionEnum.USER.getCode());
        return ResponseVO.ok();
    }

    /**
     * 用户登录
     * 验证码 + 账密校验通过后生成 Token，写入 Redis，TTL 7 天
     */
    @PostMapping("/login")
    public ResponseVO<Map<String, Object>> login(@RequestBody LoginDTO dto, HttpServletRequest request) {
        verifyCheckCode(RedisUtils.CHECK_CODE_CLIENT_PREFIX + dto.getCheckCodeKey(), dto.getCheckCode());
        String clientIp = getClientIp(request);
        UserInfoVO userInfoVO = userInfoService.login(dto.getAccount(), dto.getPassword(), clientIp);
        // 生成无状态 Token，以 UUID 作为凭证，用户信息序列化后存入 Redis
        String token = UUID.randomUUID().toString().replace("-", "");
        redisUtils.set(RedisUtils.AUTH_TOKEN_PREFIX + token, JSON.toJSONString(userInfoVO), RedisUtils.TOKEN_TTL);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", userInfoVO);
        return ResponseVO.ok(data);
    }

    /**
     * 自动登录
     * Token 由拦截器已校验，此处只刷新最后登录信息并返回最新用户数据
     */
    @PostMapping("/autoLogin")
    public ResponseVO<UserInfoVO> autoLogin(HttpServletRequest request) {
        // 用户信息由拦截器从 Redis 解析后写入 ThreadLocal，直接取用
        UserInfoVO user = UserContext.get();
        String clientIp = getClientIp(request);
        userInfoService.updateLoginInfo(user.getUserId(), clientIp);
        user.setLastLoginIp(clientIp);
        return ResponseVO.ok(user);
    }

    /**
     * 退出登录
     * 物理删除 Redis 中的 Token，使凭证立即失效
     */
    @PostMapping("/logout")
    public ResponseVO<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            redisUtils.delete(RedisUtils.AUTH_TOKEN_PREFIX + token);
        }
        UserContext.remove();
        return ResponseVO.ok();
    }

    // 校验验证码，不区分大小写；校验后立即删除，防止重复使用
    private void verifyCheckCode(String redisKey, String inputCode) {
        String correct = redisUtils.get(redisKey);
        if (correct == null || !correct.equalsIgnoreCase(inputCode)) {
            throw new BusinessException(ErrorCodeEnum.CODE_601);
        }
        redisUtils.delete(redisKey);
    }

    // 从 Authorization 头解析 Bearer Token
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    // 优先取代理头中的真实 IP，无代理时取直连 IP
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
