package com.newproject.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.newproject.web.dto.CommerceIntegration;
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
    controllers = {AdminIntegrationController.class, GlobalModelAttributes.class, PortalExceptionHandler.class},
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
class AdminIntegrationControllerTest {

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
        when(gatewayClient.listCommerceIntegrations()).thenReturn(List.of());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listRenders() throws Exception {
        mockMvc.perform(get("/admin/integrations").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Integrations Hub")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRedirectsToSuccess() throws Exception {
        CommerceIntegration integration = new CommerceIntegration();
        integration.setId(1L);
        when(gatewayClient.createCommerceIntegration(any())).thenReturn(integration);

        mockMvc.perform(post("/admin/integrations")
                .with(csrf())
                .param("code", "broadleaf-master")
                .param("displayName", "Broadleaf master")
                .param("providerType", "BROADLEAF")
                .param("syncMode", "BIDIRECTIONAL")
                .param("authType", "API_KEY")
                .param("baseUrl", "https://broadleaf.example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/integrations?saved=1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRedirectsToSuccess() throws Exception {
        CommerceIntegration integration = new CommerceIntegration();
        integration.setId(1L);
        when(gatewayClient.updateCommerceIntegration(eq(1L), any())).thenReturn(integration);

        mockMvc.perform(post("/admin/integrations/1")
                .with(csrf())
                .param("code", "broadleaf-master")
                .param("displayName", "Broadleaf master")
                .param("providerType", "BROADLEAF")
                .param("syncMode", "BIDIRECTIONAL")
                .param("authType", "API_KEY")
                .param("baseUrl", "https://broadleaf.example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/integrations?updated=1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRedirectsToErrorWhenGatewayFails() throws Exception {
        doThrow(new IllegalStateException("boom")).when(gatewayClient).createCommerceIntegration(any());

        mockMvc.perform(post("/admin/integrations")
                .with(csrf())
                .param("code", "broadleaf-master")
                .param("displayName", "Broadleaf master")
                .param("providerType", "BROADLEAF")
                .param("syncMode", "BIDIRECTIONAL")
                .param("authType", "API_KEY")
                .param("baseUrl", "https://broadleaf.example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/integrations?error=create"));
    }
}
