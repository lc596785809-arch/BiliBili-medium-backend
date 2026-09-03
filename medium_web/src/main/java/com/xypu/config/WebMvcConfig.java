package com.xypu.config;

import com.xypu.interceptor.TokenAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private TokenAuthInterceptor tokenAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/v1/client/checkCode",
                        "/api/v1/client/register",
                        "/api/v1/client/login"
                );
    }
}
