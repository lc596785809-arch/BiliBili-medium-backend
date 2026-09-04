package com.xypu.interceptor;

import com.alibaba.fastjson.JSON;
import com.xypu.context.UserContext;
import com.xypu.entity.vo.UserInfoVO;
import com.xypu.exception.ErrorCodeEnum;
import com.xypu.response.ResponseVO;
import com.xypu.utils.RedisUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * 客户端 Token 鉴权拦截器
 * 负责解析请求头中的 Bearer Token，校验 Redis 中的登录状态，并实现无感续期
 */
@Component
public class TokenAuthInterceptor implements HandlerInterceptor {

    @Resource
    private RedisUtils redisUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求不携带 Authorization 头，直接放行，由 CORS 配置负责应答预检响应头
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 从请求头提取 Token，不存在则直接拦截
        String token = extractToken(request);
        if (token == null) {
            writeUnauthorized(response);
            return false;
        }
        // 查询 Redis，Token 不存在或已过期则拦截
        String redisKey = RedisUtils.AUTH_TOKEN_PREFIX + token;
        String userJson = redisUtils.get(redisKey);
        if (userJson == null) {
            writeUnauthorized(response);
            return false;
        }
        // 无感续期：剩余有效期不足 1 天时，自动续期到 7 天
        Long ttl = redisUtils.getExpire(redisKey);
        if (ttl != null && ttl < RedisUtils.TOKEN_REFRESH_THRESHOLD) {
            redisUtils.expire(redisKey, RedisUtils.TOKEN_TTL);
        }
        // 将用户信息存入 ThreadLocal，供本次请求的后续逻辑直接使用
        UserInfoVO user = JSON.parseObject(userJson, UserInfoVO.class);
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清除 ThreadLocal，防止内存泄漏（线程池场景下尤为重要）
        UserContext.remove();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JSON.toJSONString(ResponseVO.error(ErrorCodeEnum.CODE_401)));
        }
    }
}
