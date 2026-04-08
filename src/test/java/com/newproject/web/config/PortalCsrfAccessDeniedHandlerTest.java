package com.newproject.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;

class PortalCsrfAccessDeniedHandlerTest {

    private PortalCsrfAccessDeniedHandler handler;

    @BeforeEach
    void setUp() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("common.error.csrf.expired", java.util.Locale.ENGLISH, "csrf expired");
        handler = new PortalCsrfAccessDeniedHandler(repository, messageSource);
    }

    @Test
    void redirectsBackWithFreshTokenOnCsrfError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cart/add");
        request.setScheme("https");
        request.setServerName("web-portal-ecommerce.apps.tsatech.it");
        request.setServerPort(443);
        request.addHeader("Referer", "https://web-portal-ecommerce.apps.tsatech.it/catalogo?q=test");

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
            request,
            response,
            new InvalidCsrfTokenException(new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "expected"), "actual")
        );

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("/catalogo?q=test&csrf=expired");
        assertThat(response.getCookie("XSRF-TOKEN")).isNotNull();
    }

    @Test
    void returnsJsonForAjaxCsrfError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cart/add");
        request.setScheme("https");
        request.setServerName("web-portal-ecommerce.apps.tsatech.it");
        request.setServerPort(443);
        request.addHeader("Accept", "application/json");

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
            request,
            response,
            new InvalidCsrfTokenException(new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "expected"), "actual")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("CSRF_EXPIRED");
    }
}
