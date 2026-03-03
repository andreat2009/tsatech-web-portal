package com.newproject.web.config;

import java.util.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/shop/**", "/css/**", "/js/**", "/images/**", "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/cart/**", "/checkout/**", "/account/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService())))
            .oauth2Client(Customizer.withDefaults())
            .logout(logout -> logout.logoutSuccessUrl("/"));

        return http.build();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return userRequest -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());

            Map<String, Object> realmAccess = oidcUser.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
                for (Object role : roles) {
                    mapped.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }

            Map<String, Object> resourceAccess = oidcUser.getClaim("resource_access");
            if (resourceAccess != null) {
                for (Object access : resourceAccess.values()) {
                    if (access instanceof Map<?, ?> accessMap && accessMap.get("roles") instanceof Collection<?> roles) {
                        for (Object role : roles) {
                            mapped.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }
                    }
                }
            }

            return new DefaultOidcUser(mapped, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }
}
