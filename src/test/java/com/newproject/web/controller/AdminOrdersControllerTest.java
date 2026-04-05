package com.newproject.web.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.newproject.web.dto.Order;
import com.newproject.web.dto.PagedResponse;
import com.newproject.web.dto.PublicStoreSettings;
import com.newproject.web.error.PortalExceptionHandler;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
class AdminOrdersControllerTest {

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

        Order order = new Order();
        order.setId(44L);
        order.setCustomerFirstName("Mario");
        order.setCustomerLastName("Rossi");
        order.setCustomerEmail("mario@example.com");
        order.setGuestCheckout(false);
        order.setStatus("Confirmed");
        order.setCreatedAt(OffsetDateTime.now());
        order.setTotal(new BigDecimal("10.00"));

        PagedResponse<Order> page = PagedResponse.empty(0, 20);
        page.setContent(List.of(order));
        page.setTotalElements(21);
        page.setTotalPages(2);
        page.setPage(0);
        page.setSize(20);
        page.setFirst(true);
        page.setLast(false);
        page.setEmpty(false);
        when(gatewayClient.listOrdersPage(eq(null), eq(0), eq(20))).thenReturn(page);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ordersPageRendersPaginationAndActions() throws Exception {
        mockMvc.perform(get("/admin/orders").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Mario Rossi")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/orders?page=2")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Chiudi")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Elimina")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void closeRedirectsPreservingPage() throws Exception {
        mockMvc.perform(post("/admin/orders/44/close").with(csrf()).param("page", "2"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/orders?page=2&closed=1"));

        verify(gatewayClient).updateOrderStatus(44L, "Closed");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRedirectsPreservingPage() throws Exception {
        mockMvc.perform(post("/admin/orders/44/delete").with(csrf()).param("page", "2"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/orders?page=2&deleted=1"));

        verify(gatewayClient).deleteOrder(44L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRedirectsToErrorWhenGatewayFails() throws Exception {
        doThrow(new IllegalStateException("boom")).when(gatewayClient).deleteOrder(44L);

        mockMvc.perform(post("/admin/orders/44/delete").with(csrf()).param("page", "2"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/orders?page=2&deleteError=1"));
    }
}
