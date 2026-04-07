package com.newproject.web.config;

import com.newproject.web.interceptor.AdminAuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminAuditWebConfig implements WebMvcConfigurer {
    private final AdminAuditInterceptor adminAuditInterceptor;

    public AdminAuditWebConfig(AdminAuditInterceptor adminAuditInterceptor) {
        this.adminAuditInterceptor = adminAuditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuditInterceptor)
            .addPathPatterns("/admin/**", "/amministrazione/**");
    }
}
