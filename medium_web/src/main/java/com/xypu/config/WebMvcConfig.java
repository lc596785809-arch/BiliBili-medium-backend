package com.xypu.config;

import com.xypu.interceptor.TokenAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private TokenAuthInterceptor tokenAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 客户端前端地址，如实际端口不同请同步修改
                .allowedOrigins("http://localhost:3001","http://localhost:3002","http://localhost:7072")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Authorization 头需在 allowedHeaders 中放行
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/v1/client/checkCode",
                        "/api/v1/client/register",
                        "/api/v1/client/login",
                        // 分类查询为公开接口，无需 Token
                        "/api/v1/client/category/loadCategory",
                        "/api/v1/client/category/loadRootCategory",
                        "/api/v1/client/category/loadLastLevelCategory",
                        // 图片资源与 HLS 视频资源为公开访问，无需 Token
                        "/api/v1/client/file/getResource",
                        "/api/v1/client/file/videoResource/**",
                        // Spring 错误转发路径，不拦截，否则业务异常会被包装为 401
                        "/error"
                );
    }
}
