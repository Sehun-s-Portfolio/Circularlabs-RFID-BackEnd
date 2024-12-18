package com.rfid.circularlabs_rfid_backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfigure implements WebMvcConfigurer {

    // Oauth 로그인 시 Cors에러 허용
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*") // 외부에서 들어오는 모둔 url 을 허용
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 허용되는 Method
                .allowedHeaders("*") // 허용되는 헤더
                .allowCredentials(false) // 자격증명 허용
                .maxAge(3600); // 허용 시간
    }
}
