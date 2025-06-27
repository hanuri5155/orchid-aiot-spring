package com.orchid.springbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    //CORS(Cross-origin resource sharing) 설정 추가
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://134.185.115.80:8080"          // OCI 서버 IP
                        //,"https://orchid-smart-grow.site"     // 배포될 도메인 나중에 추가
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** → /home/ubuntu/uploads/ 경로의 실제 파일로 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/home/ubuntu/uploads/");
    }
}