package com.orchid.springbackend.config; // ⭐ 본인의 프로젝트 패키지 구조에 맞게 변경하세요.

import org.springframework.context.annotation.Configuration; // @Configuration 어노테이션을 사용하기 위해 임포트
import org.springframework.web.servlet.config.annotation.CorsRegistry; // CORS 설정을 위한 CorsRegistry 클래스 임포트
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // 웹 MVC 설정을 커스터마이징하기 위한 인터페이스 임포트

@Configuration
// Spring이 애플리케이션 시작 시 이 클래스를 스캔하여 설정을 적용
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
}