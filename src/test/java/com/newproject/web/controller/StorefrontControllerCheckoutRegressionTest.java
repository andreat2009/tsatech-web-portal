package com.newproject.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.newproject.web.dto.PriceQuoteResponse;
import com.newproject.web.dto.Product;
import com.newproject.web.dto.PublicStoreSettings;
import com.newproject.web.error.PortalExceptionHandler;
import com.newproject.web.dto.PaymentMethod;
import com.newproject.web.service.CustomerResolver;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {StorefrontController.class, GlobalModelAttributes.class, PortalExceptionHandler.class},
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
class StorefrontControllerCheckoutRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayClient gatewayClient;

    @MockBean
    private CustomerResolver customerResolver;

    @BeforeEach
    void setUp() {
        PublicStoreSettings settings = new PublicStoreSettings();
        settings.setSiteName("TSATech Store");
        when(gatewayClient.getPublicStoreSettings()).thenReturn(settings);
        when(gatewayClient.listInformationPages(true)).thenReturn(List.of());
        when(customerResolver.resolveCustomerId(any())).thenReturn(null);

        Product product = new Product();
        product.setId(1004L);
        product.setName("Northwind Buds Air");
        product.setPrice(new BigDecimal("159.00"));
        when(gatewayClient.getProductSafe(1004L)).thenReturn(Optional.of(product));

        PaymentMethod method1 = new PaymentMethod();
        method1.setCode("cash_on_delivery");
        method1.setDisplayName("Cash on delivery");
        method1.setDescription("Pay when the order is delivered.");
        PaymentMethod method2 = new PaymentMethod();
        method2.setCode("bank_transfer");
        method2.setDisplayName("Bank transfer");
        method2.setDescription("Manual offline bank transfer.");
        when(gatewayClient.listPaymentMethods()).thenReturn(List.of(method1, method2));
        when(gatewayClient.listCustomFields(eq("CHECKOUT"), eq(true))).thenReturn(List.of());

        PriceQuoteResponse quote = new PriceQuoteResponse();
        quote.setSubtotal(new BigDecimal("159.00"));
        quote.setShipping(new BigDecimal("8.00"));
        quote.setDiscount(BigDecimal.ZERO);
        quote.setTotal(new BigDecimal("167.00"));
        when(gatewayClient.quote(any())).thenReturn(quote);
    }

    @Test
    void guestCheckoutPageRendersWithDynamicPaymentMethods() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("GUEST_CART_ITEMS", Map.of(1004L, 1));

        mockMvc.perform(get("/checkout-rapido").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cash on delivery")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Pay when the order is delivered.")));
    }
}
