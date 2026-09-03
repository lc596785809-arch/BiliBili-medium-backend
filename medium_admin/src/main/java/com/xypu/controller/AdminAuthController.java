package com.xypu.controller;

import com.wf.captcha.ArithmeticCaptcha;
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
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuthController {

    // Session 中存储管理员信息的 Key
    public static final String SESSION_ADMIN_KEY = "ADMIN_USER";

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private UserInfoService userInfoService;

    /**
     * 获取管理端图形验证码
     * 使用独立的 Redis Key 前缀与客户端验证码隔离
     */
    @GetMapping("/checkCode")
    public ResponseVO<Map<String, String>> checkCode() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 48);
        String answer = captcha.text();
        String base64 = captcha.toBase64();
        String key = UUID.randomUUID().toString().replace("-", "");
        redisUtils.set(RedisUtils.CHECK_CODE_ADMIN_PREFIX + key, answer, RedisUtils.CHECK_CODE_TTL);
        Map<String, String> data = new HashMap<>();
        data.put("checkCodeKey", key);
        data.put("checkCodeBase64", base64);
        return ResponseVO.ok(data);
    }

    /**
     * 管理员注册
     * 注册时强制设置 permission=3（管理员），不接受前端传入的权限值
     */
    @PostMapping("/register")
    public ResponseVO<Void> register(@RequestBody RegisterDTO dto) {
        verifyCheckCode(RedisUtils.CHECK_CODE_ADMIN_PREFIX + dto.getCheckCodeKey(), dto.getCheckCode());
        // 固定写入管理员权限，不允许注册普通用户
        userInfoService.register(dto.getAccount(), dto.getPassword(), dto.getNickName(), PermissionEnum.ADMIN.getCode());
        return ResponseVO.ok();
    }

    /**
     * 管理员登录
     * 在账密校验通过后，必须二次校验 permission=3，否则拒绝访问后台
     * 登录态基于 HttpSession，关闭浏览器或 30 分钟无操作后自动失效
     */
    @PostMapping("/login")
    public ResponseVO<UserInfoVO> login(@RequestBody LoginDTO dto, HttpServletRequest request) {
        verifyCheckCode(RedisUtils.CHECK_CODE_ADMIN_PREFIX + dto.getCheckCodeKey(), dto.getCheckCode());
        String clientIp = getClientIp(request);
        UserInfoVO userInfoVO = userInfoService.login(dto.getAccount(), dto.getPassword(), clientIp);
        // 权限硬校验：非管理员账号禁止登录后台，即使账密正确也拒绝
        if (!PermissionEnum.ADMIN.getCode().equals(userInfoVO.getPermission())) {
            throw new BusinessException(ErrorCodeEnum.CODE_605);
        }
        // 将管理员信息写入 Session，30 分钟无操作自动过期
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_ADMIN_KEY, userInfoVO);
        session.setMaxInactiveInterval(1800);
        return ResponseVO.ok(userInfoVO);
    }

    /**
     * 管理员登出
     * 显式销毁 Session，确保会话立即失效
     */
    @PostMapping("/logout")
    public ResponseVO<Void> logout(HttpServletRequest request) {
        // getSession(false) 不创建新 Session，避免不必要的 Session 创建
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
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

    // 优先取代理头中的真实 IP，无代理时取直连 IP
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
