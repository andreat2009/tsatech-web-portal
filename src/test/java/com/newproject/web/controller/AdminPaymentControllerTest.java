package com.newproject.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.newproject.web.dto.Order;
import com.newproject.web.dto.Payment;
import com.newproject.web.dto.PaymentTransaction;
import com.newproject.web.dto.PublicStoreSettings;
import com.newproject.web.error.PortalExceptionHandler;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {AdminPaymentController.class, GlobalModelAttributes.class, PortalExceptionHandler.class},
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(ThymeleafSecurityTestConfig.class)
class AdminPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayClient gatewayClient;

    @BeforeEach
    void setUp() {
        PublicStoreSettings settings = new PublicStoreSettings();
        settings.setSiteName("TSATech Store");
        when(gatewayClient.getPublicStoreSettings()).thenReturn(settings);
        when(gatewayClient.listInformationPages(true)).thenReturn(List.of());
        when(gatewayClient.listPayments(any(), any(), any(), any(), any())).thenReturn(List.of(samplePayment()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listRendersOperationalConsole() throws Exception {
        mockMvc.perform(get("/admin/payments").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Export CSV")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Monitor provider status")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Open")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("PP-ORDER-1")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void detailRendersTimelineAndOperations() throws Exception {
        Payment payment = samplePayment();
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(99L);
        transaction.setOperationType("CAPTURE");
        transaction.setEventSource("PAYPAL_WEBHOOK");
        transaction.setStatus("CAPTURED");
        transaction.setProviderReference("CAPTURE-1");
        transaction.setAmount(new BigDecimal("19.90"));
        transaction.setCurrency("EUR");
        transaction.setCreatedAt(OffsetDateTime.now());
        Order order = new Order();
        order.setId(10L);
        order.setCustomerEmail("customer@example.com");
        order.setStatus("Confirmed");

        when(gatewayClient.getPayment(1L)).thenReturn(payment);
        when(gatewayClient.listPaymentTransactions(1L)).thenReturn(List.of(transaction));
        when(gatewayClient.getOrderSafe(10L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/admin/payments/1").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment timeline")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Run reconciliation")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("CAPTURE-1")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("customer@example.com")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportReturnsCsv() throws Exception {
        mockMvc.perform(get("/admin/payments/export").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("payments-")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("payment_id,order_id,provider")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reconcileRedirectsBackToList() throws Exception {
        mockMvc.perform(post("/admin/payments/1/reconcile").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/payments?reconciled=1"));
    }

    private Payment samplePayment() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(10L);
        payment.setProvider("PAYPAL");
        payment.setMethodCode("paypal");
        payment.setMethodLabel("PayPal");
        payment.setStatus("CAPTURED");
        payment.setProviderStatus("COMPLETED");
        payment.setProviderOrderId("PP-ORDER-1");
        payment.setAmount(new BigDecimal("19.90"));
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setCurrency("EUR");
        payment.setCreatedAt(OffsetDateTime.parse("2026-04-08T10:00:00Z"));
        payment.setUpdatedAt(OffsetDateTime.parse("2026-04-08T10:05:00Z"));
        return payment;
    }
}
