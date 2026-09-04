package com.xypu.interceptor;

import com.alibaba.fastjson.JSON;
import com.xypu.controller.AdminAuthController;
import com.xypu.exception.ErrorCodeEnum;
import com.xypu.response.ResponseVO;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;

/**
 * 管理端 Session 鉴权拦截器
 * 每次请求校验 Session 中是否存在有效的管理员信息，无 Session 或已过期则拒绝访问
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求规范上不携带 Cookie，直接放行，由 CORS 配置负责应答预检响应头
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // getSession(false)：不自动创建新 Session，只检查是否已有有效 Session
        HttpSession session = request.getSession(false);
        // Session 不存在（未登录/浏览器关闭/超时）或 Session 中无管理员信息，拦截请求
        if (session == null || session.getAttribute(AdminAuthController.SESSION_ADMIN_KEY) == null) {
            writeUnauthorized(response);
            return false;
        }
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JSON.toJSONString(ResponseVO.error(ErrorCodeEnum.CODE_401)));
        }
    }
}
