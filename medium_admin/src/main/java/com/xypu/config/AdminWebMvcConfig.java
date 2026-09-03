package com.xypu.config;

import com.xypu.interceptor.AdminAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;


@Configuration
public class AdminWebMvcConfig implements WebMvcConfigurer {

    @Resource
    private AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                //拦截规则，配置为拦截该应用下的所有请求路径。
                .addPathPatterns("/**")
                //放行路径，以下三个接口不需要校验 Session
                .excludePathPatterns(
                        "/api/v1/admin/checkCode",
                        "/api/v1/admin/register",
                        "/api/v1/admin/login"
                );
    }
}
