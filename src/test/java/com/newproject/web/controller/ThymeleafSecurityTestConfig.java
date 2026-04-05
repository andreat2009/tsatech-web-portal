package com.newproject.web.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;

@TestConfiguration
class ThymeleafSecurityTestConfig {

    @Bean
    SecurityExpressionHandler<FilterInvocation> securityExpressionHandler() {
        return new DefaultWebSecurityExpressionHandler();
    }
}
