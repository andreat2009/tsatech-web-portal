package com.newproject.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.newproject.web.dto.AnalyticsSummary;
import com.newproject.web.dto.PublicStoreSettings;
import com.newproject.web.error.PortalExceptionHandler;
import com.newproject.web.service.GatewayClient;
import java.util.List;
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
    controllers = {AdminController.class, GlobalModelAttributes.class, PortalExceptionHandler.class},
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
class AdminDashboardControllerTest {

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
        when(gatewayClient.listProducts()).thenReturn(List.of());
        when(gatewayClient.listCategories(eq(null))).thenReturn(List.of());
        when(gatewayClient.listManufacturers()).thenReturn(List.of());
        when(gatewayClient.listCustomers()).thenReturn(List.of());
        when(gatewayClient.listCustomFields(eq(null), eq(null))).thenReturn(List.of());
        when(gatewayClient.listCarts(eq(null))).thenReturn(List.of());
        when(gatewayClient.listOrders(eq(null))).thenReturn(List.of());
        when(gatewayClient.listInventory()).thenReturn(List.of());
        when(gatewayClient.listPrices()).thenReturn(List.of());
        when(gatewayClient.listCoupons()).thenReturn(List.of());
        when(gatewayClient.listPayments(eq(null))).thenReturn(List.of());
        when(gatewayClient.listAdminPaymentMethods()).thenReturn(List.of());
        when(gatewayClient.listShipments(eq(null))).thenReturn(List.of());
        when(gatewayClient.listReturns(eq(null), eq(null))).thenReturn(List.of());
        when(gatewayClient.listInformationPages(eq(null))).thenReturn(List.of());
        when(gatewayClient.listBlogPosts(eq(null))).thenReturn(List.of());
        when(gatewayClient.listBlogComments(eq(null))).thenReturn(List.of());
        when(gatewayClient.listContactMessages(eq(null))).thenReturn(List.of());
        when(gatewayClient.getAnalyticsSummary()).thenReturn(new AnalyticsSummary());
        when(gatewayClient.listAnalyticsEvents(any())).thenReturn(List.of());
        when(gatewayClient.notificationPing()).thenReturn("ok");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardRenders() throws Exception {
        mockMvc.perform(get("/admin").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard Admin")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Custom fields")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Payment Credentials")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Manage payment methods & credentials")));
    }
}
