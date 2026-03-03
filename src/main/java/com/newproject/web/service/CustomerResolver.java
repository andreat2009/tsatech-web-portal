package com.newproject.web.service;

import com.newproject.web.dto.Customer;
import com.newproject.web.dto.CustomerRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class CustomerResolver {
    private static final Logger logger = LoggerFactory.getLogger(CustomerResolver.class);

    private final GatewayClient gatewayClient;

    public CustomerResolver(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    public Customer resolveCurrentCustomer(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            return null;
        }

        try {
            OidcUser user = (OidcUser) authentication.getPrincipal();
            String email = user.getEmail();
            String preferredUsername = user.getPreferredUsername();
            String keycloakUserId = user.getSubject();

            if (keycloakUserId != null && !keycloakUserId.isBlank()) {
                List<Customer> byKeycloak = gatewayClient.listCustomers(null, keycloakUserId, null);
                if (!byKeycloak.isEmpty()) {
                    return byKeycloak.get(0);
                }
            }

            if (email != null && !email.isBlank()) {
                List<Customer> byEmail = gatewayClient.listCustomers(email, null, null);
                if (!byEmail.isEmpty()) {
                    return byEmail.get(0);
                }
            }

            CustomerRequest request = new CustomerRequest();
            request.setActive(true);
            request.setKeycloakUserId(keycloakUserId);
            request.setEmail(email != null ? email.toLowerCase(Locale.ROOT) : preferredUsername + "@example.local");
            request.setFirstName(user.getGivenName());
            request.setLastName(user.getFamilyName());
            return gatewayClient.createCustomer(request);
        } catch (Exception ex) {
            logger.warn("Unable to resolve/create customer for authenticated principal: {}", ex.getMessage());
            return null;
        }
    }

    public Long resolveCustomerId(Authentication authentication) {
        Customer customer = resolveCurrentCustomer(authentication);
        return customer != null ? customer.getId() : null;
    }
}
