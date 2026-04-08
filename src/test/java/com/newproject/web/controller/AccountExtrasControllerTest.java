package com.newproject.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.newproject.web.dto.Customer;
import com.newproject.web.dto.CustomerRequest;
import com.newproject.web.dto.PayPalBrowserVaultSession;
import com.newproject.web.dto.PayPalSetupToken;
import com.newproject.web.dto.PaymentInstrument;
import com.newproject.web.dto.PaymentMethod;
import com.newproject.web.dto.PublicStoreSettings;
import com.newproject.web.error.PortalExceptionHandler;
import com.newproject.web.service.CustomerResolver;
import com.newproject.web.service.GatewayClient;
import com.newproject.web.service.KeycloakRegistrationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    controllers = {AccountExtrasController.class, GlobalModelAttributes.class, PortalExceptionHandler.class},
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
class AccountExtrasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayClient gatewayClient;

    @MockBean
    private CustomerResolver customerResolver;

    @MockBean
    private KeycloakRegistrationService keycloakRegistrationService;

    @BeforeEach
    void setUp() {
        PublicStoreSettings settings = new PublicStoreSettings();
        settings.setSiteName("TSATech Store");
        when(gatewayClient.getPublicStoreSettings()).thenReturn(settings);
        when(gatewayClient.listInformationPages(true)).thenReturn(List.of());
        when(gatewayClient.listPaymentMethods()).thenReturn(List.of());
    }

    @Test
    void registerPageRendersPrivacyNotice() throws Exception {
        mockMvc.perform(get("/account/register").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("privacyAccepted")));
    }

    @Test
    void registerRejectsMissingPrivacyAcceptance() throws Exception {
        mockMvc.perform(post("/account/register")
                .with(csrf())
                .param("email", "shopper@example.com")
                .param("password", "Secret123!")
                .param("passwordConfirm", "Secret123!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account/register?error=privacy"));
    }

    @Test
    void registerCreatesRetailCustomerProfile() throws Exception {
        when(keycloakRegistrationService.createUserWithRole(any())).thenReturn("kc-user-1");
        Customer created = new Customer();
        created.setId(77L);
        when(gatewayClient.createCustomer(any())).thenReturn(created);

        mockMvc.perform(post("/account/register")
                .with(csrf())
                .param("email", "Shopper@Example.com")
                .param("password", "Secret123!")
                .param("passwordConfirm", "Secret123!")
                .param("privacyAccepted", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account/register?success=1"));

        ArgumentCaptor<CustomerRequest> captor = ArgumentCaptor.forClass(CustomerRequest.class);
        verify(gatewayClient).createCustomer(captor.capture());
        CustomerRequest request = captor.getValue();
        assertEquals("kc-user-1", request.getKeycloakUserId());
        assertEquals("shopper@example.com", request.getEmail());
        assertEquals("RETAIL", request.getCustomerGroupCode());
        assertNotNull(request.getPrivacyAcceptedAt());
        assertEquals("2026-04", request.getPrivacyPolicyVersion());
    }

    @Test
    @WithMockUser(username = "shopper", roles = "USER")
    void editProfileRendersAccountSections() throws Exception {
        when(customerResolver.resolveCustomerId(any())).thenReturn(42L);
        Customer customer = new Customer();
        customer.setId(42L);
        customer.setEmail("shopper@example.com");
        when(gatewayClient.getCustomerSafe(42L)).thenReturn(java.util.Optional.of(customer));
        when(gatewayClient.listCustomerAddresses(42L)).thenReturn(List.of());
        when(gatewayClient.listPaymentMethods()).thenReturn(List.of());
        when(gatewayClient.listPaymentInstruments(42L)).thenReturn(List.of());

        mockMvc.perform(get("/account/edit").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("shippingLine1")))
            .andExpect(content().string(containsString("billingLine1")));
    }

    @Test
    @WithMockUser(username = "shopper", roles = "USER")
    void paymentInstrumentRequiresProviderToken() throws Exception {
        when(customerResolver.resolveCustomerId(any())).thenReturn(42L);

        mockMvc.perform(post("/account/payment-method/instruments")
                .with(csrf())
                .param("paymentMethodCode", "paypal"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account/payment-method?error=data"));
    }

    @Test
    @WithMockUser(username = "shopper", roles = "USER")
    void paymentMethodPageRendersTokenVault() throws Exception {
        when(customerResolver.resolveCustomerId(any())).thenReturn(42L);
        Customer customer = new Customer();
        customer.setId(42L);
        customer.setEmail("shopper@example.com");
        customer.setPreferredPaymentMethodCode("paypal");
        when(gatewayClient.getCustomerSafe(42L)).thenReturn(java.util.Optional.of(customer));

        PaymentMethod method = new PaymentMethod();
        method.setCode("paypal");
        method.setDisplayName("PayPal");
        method.setProvider("PAYPAL");
        method.setPaymentFlow("REDIRECT");
        method.setActive(true);
        method.setProviderConfigurationAvailable(true);
        method.setBrowserTokenizationMode("PAYPAL_JS_SDK");
        when(gatewayClient.listPaymentMethods()).thenReturn(List.of(method));
        when(gatewayClient.listCustomerAddresses(42L)).thenReturn(List.of());
        when(gatewayClient.listPaymentInstruments(42L)).thenReturn(List.<PaymentInstrument>of());

        mockMvc.perform(get("/account/payment-method").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-payment-vault-form")))
            .andExpect(content().string(containsString("data-payment-sdk-field")))
            .andExpect(content().string(containsString("Token vault pagamenti")));
    }

    @Test
    @WithMockUser(username = "shopper", roles = "USER")
    void paypalBrowserVaultSessionEndpointUsesResolvedCustomer() throws Exception {
        when(customerResolver.resolveCustomerId(any())).thenReturn(42L);
        PayPalBrowserVaultSession session = new PayPalBrowserVaultSession();
        session.setPaymentMethodCode("paypal");
        session.setClientId("client-id");
        session.setUserIdToken("id-token");
        session.setSdkUrl("https://www.paypal.com/sdk/js");
        when(gatewayClient.createPayPalBrowserVaultSession(42L, "paypal")).thenReturn(session);

        mockMvc.perform(post("/account/payment-method/providers/paypal/paypal/browser-session").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("client-id")));
    }

    @Test
    @WithMockUser(username = "shopper", roles = "USER")
    void paypalSetupTokenEndpointUsesResolvedCustomer() throws Exception {
        when(customerResolver.resolveCustomerId(any())).thenReturn(42L);
        PayPalSetupToken token = new PayPalSetupToken();
        token.setPaymentMethodCode("paypal");
        token.setSetupToken("setup-token-1");
        when(gatewayClient.createPayPalSetupToken(42L, "paypal")).thenReturn(token);

        mockMvc.perform(post("/account/payment-method/providers/paypal/paypal/setup-token").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("setup-token-1")));
    }
}
