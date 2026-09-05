package com.xypu.config;

import com.xypu.interceptor.AdminAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;


@Configuration
public class AdminWebMvcConfig implements WebMvcConfigurer {

    @Value("${project.folder}")
    private String projectFolder;

    @Resource
    private AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /images/** 映射到 project.folder/images/ 目录，使上传的图片可通过 URL 直接访问
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + projectFolder + "images/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 管理后台前端地址，allowCredentials 为 true 时不可使用通配符
                .allowedOrigins("http://localhost:3002","http://localhost:3001")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // Session Cookie 需要跨域携带，必须设置为 true
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                //拦截规则，配置为拦截该应用下的所有请求路径。
                .addPathPatterns("/**")
                //放行路径，以下三个接口不需要校验 Session
                .excludePathPatterns(
                        "/api/v1/admin/checkCode",
                        "/api/v1/admin/register",
                        "/api/v1/admin/login",
                        // 视频播放测试页（静态资源，无需登录）
                        "/player.html"
                );
    }
}
