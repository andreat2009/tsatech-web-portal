package com.newproject.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.newproject.web.dto.InventoryItem;
import com.newproject.web.dto.Product;
import com.newproject.web.dto.ProductVariant;
import com.newproject.web.dto.PublicStoreSettings;
import com.newproject.web.error.PortalExceptionHandler;
import com.newproject.web.service.GatewayClient;
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
    controllers = {AdminInventoryController.class, GlobalModelAttributes.class, PortalExceptionHandler.class},
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
class AdminInventoryControllerTest {

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
        when(gatewayClient.listProducts()).thenReturn(List.of(sampleProduct()));
        when(gatewayClient.listInventory()).thenReturn(List.of(sampleInventory()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listRendersOperationalConsole() throws Exception {
        mockMvc.perform(get("/admin/inventory").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Control room inventario")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("TSA T-Shirt")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Riservato nei checkout")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Rilascia riservato")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void detailRendersAdjustmentConsole() throws Exception {
        mockMvc.perform(get("/admin/inventory/1").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Correggi stock")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Size M")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Rilascia riservato")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportReturnsCsv() throws Exception {
        mockMvc.perform(get("/admin/inventory/export").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inventory-")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("inventory_id,product_id,product_name")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void releaseRedirectsBackToList() throws Exception {
        mockMvc.perform(post("/admin/inventory/1/release-reserved").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/inventory?released=1"));
    }

    private Product sampleProduct() {
        Product product = new Product();
        product.setId(1001L);
        product.setName("TSA T-Shirt");
        product.setSku("TSA-TSHIRT");
        ProductVariant variant = new ProductVariant();
        variant.setVariantKey("size-m");
        variant.setDisplayName("Size M");
        product.setVariants(List.of(variant));
        return product;
    }

    private InventoryItem sampleInventory() {
        InventoryItem item = new InventoryItem();
        item.setId(1L);
        item.setProductId(1001L);
        item.setVariantKey("size-m");
        item.setOnHand(3);
        item.setReserved(2);
        item.setUpdatedAt(OffsetDateTime.parse("2026-04-08T12:00:00Z"));
        return item;
    }
}
