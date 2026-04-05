package com.newproject.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.newproject.web.dto.Order;
import com.newproject.web.dto.PagedResponse;
import com.newproject.web.dto.PublicStoreSettings;
import com.newproject.web.error.PortalExceptionHandler;
import com.newproject.web.service.CustomerResolver;
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
class StorefrontOrdersPaginationTest {

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
        when(customerResolver.resolveCustomerId(any())).thenReturn(7L);

        Order order = new Order();
        order.setId(99L);
        order.setStatus("PAID");
        order.setCreatedAt(OffsetDateTime.now());
        order.setTotal(new BigDecimal("20.00"));

        PagedResponse<Order> page = PagedResponse.empty(1, 10);
        page.setContent(List.of(order));
        page.setTotalElements(12);
        page.setTotalPages(2);
        page.setPage(1);
        page.setSize(10);
        page.setFirst(false);
        page.setLast(true);
        page.setEmpty(false);
        when(gatewayClient.listOrdersPage(7L, 1, 10)).thenReturn(page);
    }

    @Test
    void accountOrdersRendersServerSidePagination() throws Exception {
        mockMvc.perform(get("/account/ordini").param("page", "2"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("99")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/account/ordini?page=1")));
    }
}
