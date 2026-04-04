package com.newproject.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newproject.web.dto.Cart;
import com.newproject.web.dto.CartRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;

class GatewayClientAuthHeaderTest {

    private HttpServer server;
    private final AtomicReference<String> authorizationHeader = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/carts", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
                byte[] body = "{\"id\":99,\"customerId\":1,\"status\":\"OPEN\",\"currency\":\"EUR\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(201, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createCartUsesBearerTokenFromAuthorizedClientRepository() {
        OAuth2AuthorizedClientRepository repository = mock(OAuth2AuthorizedClientRepository.class);
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
            .tokenUri("https://example.test/token")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientId("web-portal")
            .authorizationUri("https://example.test/auth")
            .redirectUri("https://example.test/callback")
            .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "test-access-token",
            java.time.Instant.now(),
            java.time.Instant.now().plusSeconds(300)
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
            new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("sub", "user-1"), "sub"),
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            "keycloak"
        );
        when(repository.loadAuthorizedClient(org.mockito.ArgumentMatchers.eq("keycloak"), org.mockito.ArgumentMatchers.eq(authentication), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new OAuth2AuthorizedClient(registration, authentication.getName(), accessToken));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        GatewayClient gatewayClient = new GatewayClient(
            WebClient.builder().build(),
            WebClient.builder().build(),
            repository,
            "http://localhost:" + server.getAddress().getPort(),
            1000
        );

        CartRequest request = new CartRequest();
        request.setCustomerId(1L);
        request.setCurrency("EUR");
        request.setStatus("OPEN");

        Cart cart = gatewayClient.createCart(request);

        assertThat(cart.getId()).isEqualTo(99L);
        assertThat(authorizationHeader.get()).isEqualTo("Bearer test-access-token");
    }
}
