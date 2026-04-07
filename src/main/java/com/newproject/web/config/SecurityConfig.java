package com.newproject.web.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline' https:; script-src 'self'; connect-src 'self'; font-src 'self' data: https:; object-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/actuator/health", "/actuator/info", "/analytics/track")
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .permissionsPolicy(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()"))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/shop/**",
                    "/catalogo/**",
                    "/product/**",
                    "/information/**",
                    "/contatti",
                    "/mappa-sito",
                    "/blog/**",
                    "/news/**",
                    "/cart/**",
                    "/carrello",
                    "/checkout/**",
                    "/checkout-rapido",
                    "/checkout/confermato",
                    "/account/register",
                    "/account/register/start",
                    "/account/login",
                    "/account/forgotten",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()
                .requestMatchers("/admin/**", "/amministrazione/**").hasRole("ADMIN")
                .requestMatchers("/account/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/account/login")
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
            )
            .oauth2Client(Customizer.withDefaults())
            .logout(logout -> logout
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
            );

        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        return userRequest -> {
            Set<GrantedAuthority> mapped = new HashSet<>();
            OidcUser oidcUser = new DefaultOidcUser(
                mapped,
                userRequest.getIdToken(),
                "preferred_username"
            );

            extractRoles(oidcUser.getClaims(), mapped);
            Map<String, Object> accessClaims = decodeAccessTokenClaims(userRequest.getAccessToken().getTokenValue());
            extractRoles(accessClaims, mapped);
            ensureUserRole(mapped);

            return new DefaultOidcUser(mapped, userRequest.getIdToken(), "preferred_username");
        };
    }

    private void extractRoles(Map<String, Object> claims, Set<GrantedAuthority> mapped) {
        if (claims == null || claims.isEmpty()) {
            return;
        }

        Object realmAccessObj = claims.get("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof Collection<?> roles) {
                for (Object role : roles) {
                    addRoleAuthority(mapped, role);
                }
            }
        }

        Object resourceAccessObj = claims.get("resource_access");
        if (resourceAccessObj instanceof Map<?, ?> resourceAccess) {
            for (Object access : resourceAccess.values()) {
                if (access instanceof Map<?, ?> accessMap) {
                    Object rolesObj = accessMap.get("roles");
                    if (rolesObj instanceof Collection<?> roles) {
                        for (Object role : roles) {
                            addRoleAuthority(mapped, role);
                        }
                    }
                }
            }
        }
    }

    private void ensureUserRole(Set<GrantedAuthority> mapped) {
        boolean hasUserRole = mapped.stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_USER"::equals);

        if (!hasUserRole) {
            mapped.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    private void addRoleAuthority(Set<GrantedAuthority> mapped, Object roleObj) {
        if (roleObj == null) {
            return;
        }
        String role = roleObj.toString().trim();
        if (role.isEmpty()) {
            return;
        }

        mapped.add(new SimpleGrantedAuthority("ROLE_" + role));
        mapped.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
    }

    private Map<String, Object> decodeAccessTokenClaims(String jwtTokenValue) {
        try {
            String[] parts = jwtTokenValue.split("\\.");
            if (parts.length < 2) {
                return Map.of();
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
