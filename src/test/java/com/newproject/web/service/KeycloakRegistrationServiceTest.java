package com.newproject.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.newproject.web.dto.CustomerRegistrationForm;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class KeycloakRegistrationServiceTest {

    private HttpServer server;
    private List<String> calls;

    @BeforeEach
    void setUp() throws Exception {
        calls = new CopyOnWriteArrayList<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/realms/master/protocol/openid-connect/token", exchange -> {
            calls.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            respondJson(exchange, 200, "{\"access_token\":\"admin-token\"}");
        });
        server.createContext("/admin/realms/master/users", new UsersHandler());
        server.createContext("/admin/realms/master/users/user-1/reset-password", exchange -> respondEmpty(exchange, 204));
        server.createContext("/admin/realms/master/users/user-1/role-mappings/realm", exchange -> respondEmpty(exchange, 204));
        server.createContext("/admin/realms/master/roles/USER", exchange -> {
            calls.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            if (calls.stream().filter(call -> call.contains("POST /admin/realms/master/roles")).findAny().isPresent()) {
                respondJson(exchange, 200, "{\"id\":\"role-user\",\"name\":\"USER\"}");
            } else {
                respondJson(exchange, 404, "{\"error\":\"not found\"}");
            }
        });
        server.createContext("/admin/realms/master/roles", exchange -> {
            calls.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            respondEmpty(exchange, 201);
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createUserWithRoleAutoCreatesMissingRealmRole() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        KeycloakRegistrationService service = new KeycloakRegistrationService(
            WebClient.builder().build(),
            baseUrl + "/realms/master",
            baseUrl,
            "admin-cli",
            "admin",
            "adminPass",
            "USER"
        );

        CustomerRegistrationForm form = new CustomerRegistrationForm();
        form.setEmail("shopper@example.com");
        form.setPassword("Secret123!");
        form.setPasswordConfirm("Secret123!");
        form.setPrivacyAccepted(Boolean.TRUE);

        String userId = service.createUserWithRole(form);
        assertEquals("user-1", userId);
    }

    private void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void respondEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private final class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            calls.add(exchange.getRequestMethod() + " " + path + (query != null ? "?" + query : ""));
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Location", "http://localhost/admin/realms/master/users/user-1");
                respondEmpty(exchange, 201);
                return;
            }
            respondJson(exchange, 200, "[]");
        }
    }
}
