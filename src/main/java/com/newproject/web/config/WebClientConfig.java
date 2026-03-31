package com.newproject.web.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
        @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String issuerUri,
        @Value("${spring.security.oauth2.client.provider.keycloak.authorization-uri:}") String authorizationUri,
        @Value("${spring.security.oauth2.client.provider.keycloak.token-uri:}") String tokenUri,
        @Value("${spring.security.oauth2.client.provider.keycloak.user-info-uri:}") String userInfoUri,
        @Value("${spring.security.oauth2.client.provider.keycloak.jwk-set-uri:}") String jwkSetUri,
        @Value("${spring.security.oauth2.client.provider.keycloak.user-name-attribute:preferred_username}") String userNameAttribute,
        @Value("${app.keycloak-backchannel-base-url:}") String keycloakBackchannelBaseUrl,
        @Value("${spring.security.oauth2.client.registration.keycloak.client-id}") String clientId,
        @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}") String clientSecret,
        @Value("${spring.security.oauth2.client.registration.keycloak.redirect-uri}") String redirectUri,
        @Value("${spring.security.oauth2.client.registration.keycloak.scope:openid,profile,email}") String scopeCsv
    ) {
        List<String> scopes = Arrays.stream(scopeCsv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        String normalizedIssuer = normalize(issuerUri);
        String realm = realmFromIssuer(normalizedIssuer);
        String publicBase = baseFromIssuer(normalizedIssuer);
        String endpointBase = firstNonBlank(normalizeBase(keycloakBackchannelBaseUrl), publicBase);
        String resolvedAuthorizationUri = firstNonBlank(authorizationUri, normalizedIssuer + "/protocol/openid-connect/auth");
        String resolvedTokenUri = firstNonBlank(tokenUri, oidcEndpoint(endpointBase, realm, "token"));
        String resolvedUserInfoUri = firstNonBlank(userInfoUri, oidcEndpoint(endpointBase, realm, "userinfo"));
        String resolvedJwkSetUri = firstNonBlank(jwkSetUri, oidcEndpoint(endpointBase, realm, "certs"));

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("issuer", normalizedIssuer);
        metadata.put("authorization_endpoint", resolvedAuthorizationUri);
        metadata.put("token_endpoint", resolvedTokenUri);
        metadata.put("userinfo_endpoint", resolvedUserInfoUri);
        metadata.put("jwks_uri", resolvedJwkSetUri);
        metadata.put("end_session_endpoint", normalizedIssuer + "/protocol/openid-connect/logout");

        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(redirectUri)
            .scope(scopes)
            .issuerUri(normalizedIssuer)
            .authorizationUri(resolvedAuthorizationUri)
            .tokenUri(resolvedTokenUri)
            .userInfoUri(resolvedUserInfoUri)
            .jwkSetUri(resolvedJwkSetUri)
            .userNameAttributeName(userNameAttribute)
            .providerConfigurationMetadata(metadata)
            .clientName("Keycloak")
            .build();

        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .authorizationCode()
            .refreshToken()
            .build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
            new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    @Bean
    public WebClient oauth2WebClient(
        OAuth2AuthorizedClientManager authorizedClientManager,
        @Value("${app.webclient.max-in-memory-bytes:10485760}") int maxInMemoryBytes
    ) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
            new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2.setDefaultOAuth2AuthorizedClient(true);
        return WebClient.builder()
            .apply(oauth2.oauth2Configuration())
            .exchangeStrategies(exchangeStrategies(maxInMemoryBytes))
            .build();
    }

    @Bean
    public WebClient defaultWebClient(@Value("${app.webclient.max-in-memory-bytes:10485760}") int maxInMemoryBytes) {
        return WebClient.builder()
            .exchangeStrategies(exchangeStrategies(maxInMemoryBytes))
            .build();
    }

    private String realmFromIssuer(String issuerUri) {
        int markerIdx = issuerUri.indexOf("/realms/");
        if (markerIdx < 0) {
            throw new IllegalArgumentException("Invalid Keycloak issuer URI: " + issuerUri);
        }
        String realm = issuerUri.substring(markerIdx + "/realms/".length());
        int slash = realm.indexOf('/');
        return slash >= 0 ? realm.substring(0, slash) : realm;
    }

    private String baseFromIssuer(String issuerUri) {
        int markerIdx = issuerUri.indexOf("/realms/");
        if (markerIdx < 0) {
            throw new IllegalArgumentException("Invalid Keycloak issuer URI: " + issuerUri);
        }
        return issuerUri.substring(0, markerIdx);
    }

    private String oidcEndpoint(String baseUrl, String realm, String endpoint) {
        return normalizeBase(baseUrl) + "/realms/" + realm + "/protocol/openid-connect/" + endpoint;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return normalizeBase(value);
    }

    private String normalizeBase(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private ExchangeStrategies exchangeStrategies(int maxInMemoryBytes) {
        return ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemoryBytes))
            .build();
    }
}
