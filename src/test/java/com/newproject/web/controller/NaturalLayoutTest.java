package com.newproject.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.newproject.web.dto.*;
import com.newproject.web.error.PortalExceptionHandler;
import com.newproject.web.service.CustomerResolver;
import com.newproject.web.service.GatewayClient;
import com.newproject.web.service.KeycloakRegistrationService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Rendering only: all gateways are mocked; no real services, accounts or writes. */
@WebMvcTest(
    controllers = {StorefrontController.class, AdminController.class, AdminProductController.class,
        AccountExtrasController.class, GlobalModelAttributes.class, PortalExceptionHandler.class},
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class, OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import(ThymeleafSecurityTestConfig.class)
class NaturalLayoutTest {
    @Autowired private MockMvc mvc;
    @MockBean private GatewayClient gateway;
    @MockBean private CustomerResolver customers;
    @MockBean private KeycloakRegistrationService registration;

    @BeforeEach
    void fixtures() {
        PublicStoreSettings settings = new PublicStoreSettings();
        settings.setSiteName("Negozio dal database");
        settings.setSupportEmail("support@example.test");
        settings.setSupportPhone("000 000 000");
        when(gateway.getPublicStoreSettings()).thenReturn(settings);
        when(gateway.listInformationPages(true)).thenReturn(List.of());
        when(gateway.getAnalyticsSummary()).thenReturn(new AnalyticsSummary());
        when(gateway.listAdminAuditEvents(anyInt(), anyInt())).thenReturn(PagedResponse.empty(0, 8));
        when(gateway.listOrdersPage(any(), anyInt(), anyInt())).thenReturn(PagedResponse.empty(0, 10));
        when(gateway.notificationPing()).thenReturn("ok");
        List<Product> products = List.of(product(41L, "Prodotto dinamico A", "73.25", 8),
            product(42L, "Prodotto dinamico B", "18.90", 0),
            product(43L, "Prodotto dinamico C", "120.00", 4));
        when(gateway.listProducts()).thenReturn(products);
        when(gateway.listProducts(any(), any(), eq(true), any(), any(), any())).thenReturn(products);
        when(gateway.getProductSafe(41L)).thenReturn(Optional.of(products.get(0)));
        PriceQuoteResponse quote = new PriceQuoteResponse();
        quote.setSubtotal(new BigDecimal("73.25"));
        quote.setShipping(new BigDecimal("8.00"));
        quote.setDiscount(BigDecimal.ZERO);
        quote.setTotal(new BigDecimal("81.25"));
        when(gateway.quote(any())).thenReturn(quote);
    }

    private Product product(long id, String name, String price, int quantity) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSku("TEST-" + id);
        product.setDescription("Descrizione ricevuta dal servizio catalogo, non dal template.");
        product.setPrice(new BigDecimal(price));
        product.setQuantity(quantity);
        product.setActive(true);
        return product;
    }

    @Test
    void catalogKeepsDynamicContentFormsAndStockState() throws Exception {
        String html = render("/");
        assertThat(html).contains("Negozio dal database", "Prodotto dinamico A", "73.25",
            "Descrizione ricevuta dal servizio catalogo", "action=\"/cart/add\"",
            "name=\"productId\" value=\"41\"", "name=\"_csrf\"", "disabled=\"disabled\"")
            .doesNotContain("Trova il tuo prossimo wow", "Selezione demo", "home.hero");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/catalogo", "/catalogo/prodotto/41-prodotto-dinamico-a", "/carrello", "/checkout-rapido", "/account/login", "/account/register"})
    void publicScreensUseSharedTheme(String route) throws Exception {
        render(route);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/catalogo/prodotti", "/admin/catalogo/prodotti/nuovo", "/admin/orders"})
    @WithMockUser(roles = "ADMIN")
    void adminScreensKeepTheNavigationAndSecurityFields(String route) throws Exception {
        assertThat(render(route)).contains("admin-quickbar", "action=\"/logout\"", "name=\"_csrf\"");
    }

    @Test
    void everyPageUsesOneResponsiveAssetFragmentAndSkipTarget() throws Exception {
        try (var files = Files.walk(Path.of("src/main/resources/templates"))) {
            List<Path> pages = files.filter(path -> path.toString().endsWith(".html"))
                .filter(path -> !path.toString().contains("/fragments/")).toList();
            assertThat(pages).hasSizeGreaterThan(60);
            for (Path page : pages) {
                String html = Files.readString(page);
                assertThat(html).as(page.toString()).contains("~{fragments/head :: assets}", "id=\"main-content\"")
                    .doesNotContain("/css/app.css", "home.hero");
            }
        }
    }

    private String render(String route) throws Exception {
        String html = mvc.perform(get(route).locale(Locale.ITALIAN).with(csrf())
                .sessionAttr("GUEST_CART_ITEMS", Map.of("41", 1)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("/css/natural-theme.css", "/js/app-shell.js", "id=\"main-content\"");
        assertThat(html.split("name=\"viewport\"", -1)).hasSize(2);
        assertThat(html.split("/js/app-shell.js", -1)).hasSize(2);
        // Optional review artifacts, excluded from the production package by Maven.
        if (Boolean.getBoolean("layout.preview")) {
            Path folder = Path.of("target/layout-preview");
            Files.createDirectories(folder);
            String name = route.equals("/") ? "home" : route.substring(1).replace('/', '-');
            Files.writeString(folder.resolve(name + ".html"), html);
        }
        return html;
    }
}
