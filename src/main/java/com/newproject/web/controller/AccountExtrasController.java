package com.newproject.web.controller;

import com.newproject.web.dto.*;
import com.newproject.web.service.CustomerResolver;
import com.newproject.web.service.GatewayClient;
import com.newproject.web.service.KeycloakRegistrationException;
import com.newproject.web.service.KeycloakRegistrationService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/account")
public class AccountExtrasController {
    private final GatewayClient gatewayClient;
    private final CustomerResolver customerResolver;
    private final String keycloakAccountUrl;
    private final String currency;
    private final KeycloakRegistrationService keycloakRegistrationService;

    public AccountExtrasController(
        GatewayClient gatewayClient,
        CustomerResolver customerResolver,
        KeycloakRegistrationService keycloakRegistrationService,
        @Value("${app.keycloak-account-url:}") String keycloakAccountUrl,
        @Value("${app.currency:EUR}") String currency
    ) {
        this.gatewayClient = gatewayClient;
        this.customerResolver = customerResolver;
        this.keycloakAccountUrl = keycloakAccountUrl;
        this.currency = currency;
        this.keycloakRegistrationService = keycloakRegistrationService;
    }


    @GetMapping("/login")
    public String login() {
        return "redirect:/oauth2/authorization/keycloak";
    }

    @GetMapping("/register")
    public String register(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/account";
        }

        CustomerRegistrationForm form = new CustomerRegistrationForm();
        form.setNewsletter(false);
        model.addAttribute("registrationForm", form);
        return "account/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@ModelAttribute("registrationForm") CustomerRegistrationForm form) {
        if (isBlank(form.getEmail())
            || isBlank(form.getPassword())
            || isBlank(form.getPasswordConfirm())
            || isBlank(form.getFirstName())
            || isBlank(form.getLastName())
            || isBlank(form.getAddressLine1())
            || isBlank(form.getCity())
            || isBlank(form.getCountry())
            || isBlank(form.getPostalCode())) {
            return "redirect:/account/register?error=data";
        }

        if (form.getPassword().length() < 8 || !form.getPassword().equals(form.getPasswordConfirm())) {
            return "redirect:/account/register?error=password";
        }

        String keycloakUserId;
        try {
            keycloakUserId = keycloakRegistrationService.createUserWithRole(form);
        } catch (KeycloakRegistrationException ex) {
            if ("exists".equals(ex.getReason())) {
                return "redirect:/account/register?error=exists";
            }
            return "redirect:/account/register?error=identity";
        }

        CustomerRequest request = new CustomerRequest();
        request.setKeycloakUserId(keycloakUserId);
        request.setEmail(form.getEmail().trim().toLowerCase(Locale.ROOT));
        request.setFirstName(form.getFirstName().trim());
        request.setLastName(form.getLastName().trim());
        request.setPhone(form.getPhone() != null ? form.getPhone().trim() : null);
        request.setNewsletter(Boolean.TRUE.equals(form.getNewsletter()));
        request.setActive(true);

        try {
            Customer created = gatewayClient.createCustomer(request);
            if (created == null || created.getId() == null) {
                keycloakRegistrationService.deleteUserQuietly(keycloakUserId);
                return "redirect:/account/register?error=processing";
            }

            AddressRequest addressRequest = new AddressRequest();
            addressRequest.setLine1(form.getAddressLine1().trim());
            addressRequest.setLine2(form.getAddressLine2() != null ? form.getAddressLine2().trim() : null);
            addressRequest.setCity(form.getCity().trim());
            addressRequest.setRegion(form.getRegion() != null ? form.getRegion().trim() : null);
            addressRequest.setCountry(form.getCountry().trim());
            addressRequest.setPostalCode(form.getPostalCode().trim());
            addressRequest.setIsDefault(true);

            gatewayClient.createCustomerAddress(created.getId(), addressRequest);
        } catch (Exception ex) {
            keycloakRegistrationService.deleteUserQuietly(keycloakUserId);
            return "redirect:/account/register?error=processing";
        }

        return "redirect:/account/register?success=1";
    }

    @GetMapping("/register/start")
    public String registerStart() {
        return "redirect:/account/register";
    }

    @GetMapping("/forgotten")
    public String forgotten() {
        return "redirect:/account/password";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }


    @GetMapping
    public String accountHome(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        Optional<Customer> customerOpt = gatewayClient.getCustomerSafe(customerId);
        if (customerOpt.isEmpty()) {
            return "redirect:/";
        }

        Customer customer = customerOpt.get();
        NewsletterPreference newsletterPreference = gatewayClient.getNewsletterPreference(customerId);

        model.addAttribute("customer", customer);
        model.addAttribute("newsletterSubscribed", Boolean.TRUE.equals(newsletterPreference.getSubscribed()));
        model.addAttribute("rewardSummary", gatewayClient.getRewardSummary(customerId));
        model.addAttribute("latestOrders", gatewayClient.listOrders(customerId).stream().limit(5).toList());
        model.addAttribute("subscriptionCount", gatewayClient.listSubscriptions(customerId).size());
        model.addAttribute("downloadCount", gatewayClient.listDownloads(customerId).size());
        return "account/home";
    }

    @GetMapping("/edit")
    public String editProfile(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        Customer customer = gatewayClient.getCustomerSafe(customerId).orElse(null);
        if (customer == null) {
            return "redirect:/account";
        }

        CustomerRequest form = new CustomerRequest();
        form.setKeycloakUserId(customer.getKeycloakUserId());
        form.setEmail(customer.getEmail());
        form.setFirstName(customer.getFirstName());
        form.setLastName(customer.getLastName());
        form.setPhone(customer.getPhone());
        form.setActive(Boolean.TRUE.equals(customer.getActive()));

        model.addAttribute("profileForm", form);
        return "account/edit";
    }

    @PostMapping("/edit")
    public String saveProfile(@ModelAttribute("profileForm") CustomerRequest form, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        Customer current = gatewayClient.getCustomerSafe(customerId).orElse(null);
        if (current == null) {
            return "redirect:/account";
        }

        CustomerRequest update = new CustomerRequest();
        update.setKeycloakUserId(current.getKeycloakUserId());
        update.setEmail(current.getEmail());
        update.setFirstName(form.getFirstName());
        update.setLastName(form.getLastName());
        update.setPhone(form.getPhone());
        update.setActive(Boolean.TRUE.equals(current.getActive()));
        update.setNewsletter(Boolean.TRUE.equals(current.getNewsletter()));

        gatewayClient.updateCustomer(customerId, update);
        return "redirect:/account?updated=1";
    }

    @GetMapping("/newsletter")
    public String newsletter() {
        return "redirect:/account";
    }

    @PostMapping("/newsletter")
    public String updateNewsletter(@RequestParam(defaultValue = "false") boolean newsletter, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        gatewayClient.updateNewsletterPreference(customerId, newsletter);
        return "redirect:/account";
    }


    @GetMapping("/transaction")
    public String transactionAlias() {
        return "redirect:/account/transactions";
    }

    @GetMapping("/subscription")
    public String subscriptionAlias() {
        return "redirect:/account/subscriptions";
    }

    @GetMapping("/download")
    public String downloadAlias() {
        return "redirect:/account/downloads";
    }

    @GetMapping("/payment-method")
    public String paymentMethod(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }
        model.addAttribute("addresses", gatewayClient.listCustomerAddresses(customerId));
        return "account/payment-method";
    }

    @GetMapping("/reward")
    public String reward(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        model.addAttribute("rewardSummary", gatewayClient.getRewardSummary(customerId));
        model.addAttribute("rewardTransactions", gatewayClient.listRewardTransactions(customerId));
        return "account/reward";
    }

    @GetMapping("/transactions")
    public String transactions(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        model.addAttribute("transactions", gatewayClient.listStoreTransactions(customerId));
        return "account/transactions";
    }

    @GetMapping("/subscriptions")
    public String subscriptions(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        CustomerSubscriptionRequest form = new CustomerSubscriptionRequest();
        form.setStatus("ACTIVE");
        form.setCurrency(currency.toUpperCase(Locale.ROOT));
        form.setAmount(new BigDecimal("9.90"));
        form.setNextBillingAt(OffsetDateTime.now().plusMonths(1));

        model.addAttribute("subscriptions", gatewayClient.listSubscriptions(customerId));
        model.addAttribute("subscriptionForm", form);
        return "account/subscriptions";
    }

    @PostMapping("/subscriptions")
    public String addSubscription(@ModelAttribute CustomerSubscriptionRequest form, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        if (form.getPlanName() == null || form.getPlanName().isBlank()) {
            return "redirect:/account/subscriptions";
        }

        if (form.getStatus() == null || form.getStatus().isBlank()) {
            form.setStatus("ACTIVE");
        }
        if (form.getCurrency() == null || form.getCurrency().isBlank()) {
            form.setCurrency(currency.toUpperCase(Locale.ROOT));
        }
        if (form.getAmount() == null) {
            form.setAmount(BigDecimal.ZERO);
        }

        gatewayClient.createSubscription(customerId, form);
        return "redirect:/account/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/status")
    public String updateSubscriptionStatus(
        @PathVariable Long id,
        @RequestParam String status,
        Authentication authentication
    ) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        CustomerSubscription existing = gatewayClient.listSubscriptions(customerId).stream()
            .filter(subscription -> id.equals(subscription.getId()))
            .findFirst()
            .orElse(null);

        if (existing == null) {
            return "redirect:/account/subscriptions";
        }

        CustomerSubscriptionRequest request = new CustomerSubscriptionRequest();
        request.setPlanName(existing.getPlanName());
        request.setStatus(status);
        request.setAmount(existing.getAmount());
        request.setCurrency(existing.getCurrency());
        request.setNextBillingAt(existing.getNextBillingAt());
        gatewayClient.updateSubscription(customerId, id, request);

        return "redirect:/account/subscriptions";
    }

    @GetMapping("/downloads")
    public String downloads(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        model.addAttribute("downloads", gatewayClient.listDownloads(customerId));
        return "account/downloads";
    }

    @GetMapping("/password")
    public String password() {
        if (keycloakAccountUrl != null && !keycloakAccountUrl.isBlank()) {
            return "redirect:" + keycloakAccountUrl;
        }
        return "redirect:/";
    }

}
