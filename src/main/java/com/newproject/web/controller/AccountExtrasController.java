package com.newproject.web.controller;

import com.newproject.web.dto.AccountProfileForm;
import com.newproject.web.dto.Address;
import com.newproject.web.dto.AddressRequest;
import com.newproject.web.dto.Customer;
import com.newproject.web.dto.CustomerRegistrationForm;
import com.newproject.web.dto.CustomerRequest;
import com.newproject.web.dto.CustomerSubscription;
import com.newproject.web.dto.CustomerSubscriptionRequest;
import com.newproject.web.dto.NewsletterPreference;
import com.newproject.web.dto.PayPalBrowserVaultSession;
import com.newproject.web.dto.PayPalSetupToken;
import com.newproject.web.dto.PaymentInstrumentForm;
import com.newproject.web.dto.PaymentMethod;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/account")
public class AccountExtrasController {
    private static final String ADDRESS_TYPE_SHIPPING = "SHIPPING";
    private static final String ADDRESS_TYPE_BILLING = "BILLING";

    private final GatewayClient gatewayClient;
    private final CustomerResolver customerResolver;
    private final String keycloakAccountUrl;
    private final String currency;
    private final String privacyPolicyVersion;
    private final String privacyPolicyPath;
    private final KeycloakRegistrationService keycloakRegistrationService;

    public AccountExtrasController(
        GatewayClient gatewayClient,
        CustomerResolver customerResolver,
        KeycloakRegistrationService keycloakRegistrationService,
        @Value("${app.keycloak-account-url:}") String keycloakAccountUrl,
        @Value("${app.currency:EUR}") String currency,
        @Value("${app.privacy-policy-version:2026-04}") String privacyPolicyVersion,
        @Value("${app.privacy-policy-path:/information/privacy-policy}") String privacyPolicyPath
    ) {
        this.gatewayClient = gatewayClient;
        this.customerResolver = customerResolver;
        this.keycloakAccountUrl = keycloakAccountUrl;
        this.currency = currency;
        this.privacyPolicyVersion = privacyPolicyVersion;
        this.privacyPolicyPath = privacyPolicyPath;
        this.keycloakRegistrationService = keycloakRegistrationService;
    }

    @GetMapping("/login")
    public String login(Model model, Authentication authentication) {
        if (isAuthenticated(authentication)) {
            Long customerId = customerResolver.resolveCustomerId(authentication);
            if (customerId == null) {
                return handleMissingAuthenticatedCustomer(authentication);
            }
            return "redirect:/account";
        }
        model.addAttribute("loginReturnTarget", "/catalogo");
        return "account/login";
    }

    @GetMapping("/register")
    public String register(Model model, Authentication authentication) {
        if (isAuthenticated(authentication)) {
            Long customerId = customerResolver.resolveCustomerId(authentication);
            if (customerId == null) {
                return handleMissingAuthenticatedCustomer(authentication);
            }
            return "redirect:/account";
        }

        CustomerRegistrationForm form = new CustomerRegistrationForm();
        form.setPrivacyAccepted(Boolean.FALSE);
        model.addAttribute("registrationForm", form);
        model.addAttribute("privacyPolicyPath", privacyPolicyPath);
        return "account/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@ModelAttribute("registrationForm") CustomerRegistrationForm form) {
        if (isBlank(form.getEmail()) || isBlank(form.getPassword()) || isBlank(form.getPasswordConfirm())) {
            return "redirect:/account/register?error=data";
        }
        if (!Boolean.TRUE.equals(form.getPrivacyAccepted())) {
            return "redirect:/account/register?error=privacy";
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
            if ("password_policy".equals(ex.getReason())) {
                return "redirect:/account/register?error=password_policy";
            }
            return "redirect:/account/register?error=identity";
        }

        CustomerRequest request = new CustomerRequest();
        request.setKeycloakUserId(keycloakUserId);
        request.setEmail(form.getEmail().trim().toLowerCase(Locale.ROOT));
        request.setCustomerGroupCode("RETAIL");
        request.setPrivacyAcceptedAt(OffsetDateTime.now());
        request.setPrivacyPolicyVersion(privacyPolicyVersion);
        request.setActive(true);

        try {
            Customer created = gatewayClient.createCustomer(request);
            if (created == null || created.getId() == null) {
                keycloakRegistrationService.deleteUserQuietly(keycloakUserId);
                return "redirect:/account/register?error=processing";
            }
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

    @GetMapping
    public String accountHome(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return handleMissingAuthenticatedCustomer(authentication);
        }

        Optional<Customer> customerOpt = gatewayClient.getCustomerSafe(customerId);
        if (customerOpt.isEmpty()) {
            throw accountServiceUnavailable();
        }

        Customer customer = customerOpt.get();
        NewsletterPreference newsletterPreference = gatewayClient.getNewsletterPreference(customerId);

        model.addAttribute("customer", customer);
        model.addAttribute("accountDisplayName", buildAccountDisplayName(customer));
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
            return handleMissingAuthenticatedCustomer(authentication);
        }

        Customer customer = gatewayClient.getCustomerSafe(customerId).orElse(null);
        if (customer == null) {
            throw accountServiceUnavailable();
        }

        List<Address> addresses = gatewayClient.listCustomerAddresses(customerId);
        AccountProfileForm form = buildProfileForm(customer, addresses);
        model.addAttribute("profileForm", form);
        model.addAttribute("paymentMethods", gatewayClient.listPaymentMethods());
        model.addAttribute("savedPaymentInstruments", gatewayClient.listPaymentInstruments(customerId));
        return "account/edit";
    }

    @PostMapping("/edit")
    public String saveProfile(@ModelAttribute("profileForm") AccountProfileForm form, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return handleMissingAuthenticatedCustomer(authentication);
        }

        Customer current = gatewayClient.getCustomerSafe(customerId).orElse(null);
        if (current == null) {
            throw accountServiceUnavailable();
        }

        if (!validateAddressSection(form.getShippingLine1(), form.getShippingCity(), form.getShippingCountry(), form.getShippingPostalCode())) {
            return "redirect:/account/edit?error=address";
        }
        if (!validateAddressSection(form.getBillingLine1(), form.getBillingCity(), form.getBillingCountry(), form.getBillingPostalCode())) {
            return "redirect:/account/edit?error=address";
        }

        CustomerRequest update = new CustomerRequest();
        update.setKeycloakUserId(current.getKeycloakUserId());
        update.setEmail(current.getEmail());
        update.setFirstName(trimToNull(form.getFirstName()));
        update.setLastName(trimToNull(form.getLastName()));
        update.setPhone(trimToNull(form.getPhone()));
        update.setCustomerGroupCode(current.getCustomerGroupCode());
        update.setPreferredPaymentMethodCode(trimToNull(form.getPreferredPaymentMethodCode()));
        update.setPreferredShippingMethodCode(trimToNull(form.getPreferredShippingMethodCode()));
        update.setPrivacyAcceptedAt(current.getPrivacyAcceptedAt());
        update.setPrivacyPolicyVersion(current.getPrivacyPolicyVersion());
        update.setActive(Boolean.TRUE.equals(current.getActive()));
        update.setNewsletter(Boolean.TRUE.equals(current.getNewsletter()));

        try {
            gatewayClient.updateCustomer(customerId, update);
            saveTypedAddressIfPresent(customerId, ADDRESS_TYPE_SHIPPING, form.getShippingLine1(), form.getShippingLine2(), form.getShippingCity(), form.getShippingRegion(), form.getShippingCountry(), form.getShippingPostalCode());
            saveTypedAddressIfPresent(customerId, ADDRESS_TYPE_BILLING, form.getBillingLine1(), form.getBillingLine2(), form.getBillingCity(), form.getBillingRegion(), form.getBillingCountry(), form.getBillingPostalCode());
        } catch (Exception ex) {
            return "redirect:/account/edit?error=processing";
        }

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
            return handleMissingAuthenticatedCustomer(authentication);
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
            return handleMissingAuthenticatedCustomer(authentication);
        }
        Customer customer = gatewayClient.getCustomerSafe(customerId).orElse(null);
        List<PaymentMethod> paymentMethods = gatewayClient.listPaymentMethods();
        List<PaymentMethod> tokenizablePaymentMethods = tokenizablePaymentMethods(paymentMethods);
        PaymentInstrumentForm paymentInstrumentForm = new PaymentInstrumentForm();
        paymentInstrumentForm.setPaymentMethodCode(resolveDefaultTokenizablePaymentMethodCode(customer, tokenizablePaymentMethods));
        paymentInstrumentForm.setActive(Boolean.TRUE);
        paymentInstrumentForm.setDefaultInstrument(Boolean.TRUE);

        model.addAttribute("addresses", gatewayClient.listCustomerAddresses(customerId));
        model.addAttribute("paymentMethods", paymentMethods);
        model.addAttribute("tokenizablePaymentMethods", tokenizablePaymentMethods);
        model.addAttribute("paymentInstruments", gatewayClient.listPaymentInstruments(customerId));
        model.addAttribute("preferredPaymentMethodCode", customer != null ? customer.getPreferredPaymentMethodCode() : null);
        model.addAttribute("paymentInstrumentForm", paymentInstrumentForm);
        return "account/payment-method";
    }

    @PostMapping("/payment-method/providers/paypal/{paymentMethodCode}/browser-session")
    @ResponseBody
    public ResponseEntity<PayPalBrowserVaultSession> createPayPalBrowserVaultSession(
        @PathVariable String paymentMethodCode,
        Authentication authentication
    ) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return accountApiUnavailable(authentication);
        }
        try {
            return ResponseEntity.ok(gatewayClient.createPayPalBrowserVaultSession(customerId, paymentMethodCode));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @PostMapping("/payment-method/providers/paypal/{paymentMethodCode}/setup-token")
    @ResponseBody
    public ResponseEntity<PayPalSetupToken> createPayPalSetupToken(
        @PathVariable String paymentMethodCode,
        Authentication authentication
    ) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return accountApiUnavailable(authentication);
        }
        try {
            return ResponseEntity.ok(gatewayClient.createPayPalSetupToken(customerId, paymentMethodCode));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @PostMapping("/payment-method/instruments")
    public String createPaymentInstrument(@ModelAttribute("paymentInstrumentForm") PaymentInstrumentForm form, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return handleMissingAuthenticatedCustomer(authentication);
        }
        if (isBlank(form.getPaymentMethodCode()) || isBlank(form.getProviderToken())) {
            return "redirect:/account/payment-method?error=data";
        }

        try {
            gatewayClient.createPaymentInstrument(customerId, form);
            Customer customer = gatewayClient.getCustomerSafe(customerId).orElse(null);
            if (customer != null && (Boolean.TRUE.equals(form.getDefaultInstrument()) || isBlank(customer.getPreferredPaymentMethodCode()))) {
                CustomerRequest request = new CustomerRequest();
                request.setKeycloakUserId(customer.getKeycloakUserId());
                request.setEmail(customer.getEmail());
                request.setFirstName(customer.getFirstName());
                request.setLastName(customer.getLastName());
                request.setPhone(customer.getPhone());
                request.setCustomerGroupCode(customer.getCustomerGroupCode());
                request.setPreferredPaymentMethodCode(trimToNull(form.getPaymentMethodCode()));
                request.setPreferredShippingMethodCode(customer.getPreferredShippingMethodCode());
                request.setPrivacyAcceptedAt(customer.getPrivacyAcceptedAt());
                request.setPrivacyPolicyVersion(customer.getPrivacyPolicyVersion());
                request.setActive(Boolean.TRUE.equals(customer.getActive()));
                request.setNewsletter(Boolean.TRUE.equals(customer.getNewsletter()));
                gatewayClient.updateCustomer(customerId, request);
            }
            return "redirect:/account/payment-method?saved=1";
        } catch (Exception ex) {
            return "redirect:/account/payment-method?error=processing";
        }
    }

    @PostMapping("/payment-method/instruments/{instrumentId}/delete")
    public String deletePaymentInstrument(@PathVariable Long instrumentId, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return handleMissingAuthenticatedCustomer(authentication);
        }
        try {
            gatewayClient.deletePaymentInstrument(customerId, instrumentId);
            return "redirect:/account/payment-method?deleted=1";
        } catch (Exception ex) {
            return "redirect:/account/payment-method?error=processing";
        }
    }

    @GetMapping("/reward")
    public String reward(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return handleMissingAuthenticatedCustomer(authentication);
        }

        model.addAttribute("rewardSummary", gatewayClient.getRewardSummary(customerId));
        model.addAttribute("rewardTransactions", gatewayClient.listRewardTransactions(customerId));
        return "account/reward";
    }

    @GetMapping("/transactions")
    public String transactions(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return handleMissingAuthenticatedCustomer(authentication);
        }

        model.addAttribute("transactions", gatewayClient.listStoreTransactions(customerId));
        return "account/transactions";
    }

    @GetMapping("/subscriptions")
    public String subscriptions(Model model, Authentication authentication) {
        Long customerId = customerResolver.resolveCustomerId(authentication);
        if (customerId == null) {
            return handleMissingAuthenticatedCustomer(authentication);
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
            return handleMissingAuthenticatedCustomer(authentication);
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
            return handleMissingAuthenticatedCustomer(authentication);
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
            return handleMissingAuthenticatedCustomer(authentication);
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

    private AccountProfileForm buildProfileForm(Customer customer, List<Address> addresses) {
        AccountProfileForm form = new AccountProfileForm();
        form.setFirstName(customer.getFirstName());
        form.setLastName(customer.getLastName());
        form.setPhone(customer.getPhone());
        form.setPreferredPaymentMethodCode(customer.getPreferredPaymentMethodCode());
        form.setPreferredShippingMethodCode(customer.getPreferredShippingMethodCode());
        applyAddressToForm(findAddressByType(addresses, ADDRESS_TYPE_SHIPPING).orElse(null), true, form);
        applyAddressToForm(findAddressByType(addresses, ADDRESS_TYPE_BILLING).orElse(null), false, form);
        return form;
    }

    private Optional<Address> findAddressByType(List<Address> addresses, String addressType) {
        return addresses.stream()
            .filter(address -> addressType.equalsIgnoreCase(address.getAddressType()))
            .findFirst();
    }

    private void applyAddressToForm(Address address, boolean shipping, AccountProfileForm form) {
        if (address == null) {
            return;
        }
        if (shipping) {
            form.setShippingLine1(address.getLine1());
            form.setShippingLine2(address.getLine2());
            form.setShippingCity(address.getCity());
            form.setShippingRegion(address.getRegion());
            form.setShippingCountry(address.getCountry());
            form.setShippingPostalCode(address.getPostalCode());
            return;
        }
        form.setBillingLine1(address.getLine1());
        form.setBillingLine2(address.getLine2());
        form.setBillingCity(address.getCity());
        form.setBillingRegion(address.getRegion());
        form.setBillingCountry(address.getCountry());
        form.setBillingPostalCode(address.getPostalCode());
    }

    private void saveTypedAddressIfPresent(
        Long customerId,
        String addressType,
        String line1,
        String line2,
        String city,
        String region,
        String country,
        String postalCode
    ) {
        if (isBlank(line1) && isBlank(city) && isBlank(country) && isBlank(postalCode) && isBlank(line2) && isBlank(region)) {
            return;
        }
        AddressRequest request = new AddressRequest();
        request.setAddressType(addressType);
        request.setLine1(line1.trim());
        request.setLine2(trimToNull(line2));
        request.setCity(city.trim());
        request.setRegion(trimToNull(region));
        request.setCountry(country.trim());
        request.setPostalCode(postalCode.trim());
        request.setIsDefault(Boolean.TRUE);
        gatewayClient.upsertCustomerAddressByType(customerId, addressType, request);
    }

    private boolean validateAddressSection(String line1, String city, String country, String postalCode) {
        boolean anyProvided = !isBlank(line1) || !isBlank(city) || !isBlank(country) || !isBlank(postalCode);
        if (!anyProvided) {
            return true;
        }
        return !isBlank(line1) && !isBlank(city) && !isBlank(country) && !isBlank(postalCode);
    }

    private List<PaymentMethod> tokenizablePaymentMethods(List<PaymentMethod> paymentMethods) {
        return paymentMethods.stream()
            .filter(method -> Boolean.TRUE.equals(method.getActive()))
            .filter(method -> !isBlank(method.getCode()))
            .filter(method -> !"OFFLINE".equalsIgnoreCase(method.getProvider()))
            .filter(method -> !"OFFLINE".equalsIgnoreCase(method.getPaymentFlow()))
            .filter(method -> !Boolean.FALSE.equals(method.getProviderConfigurationAvailable()))
            .toList();
    }

    private String resolveDefaultTokenizablePaymentMethodCode(Customer customer, List<PaymentMethod> tokenizablePaymentMethods) {
        String preferred = customer != null ? trimToNull(customer.getPreferredPaymentMethodCode()) : null;
        if (preferred != null) {
            for (PaymentMethod method : tokenizablePaymentMethods) {
                if (preferred.equalsIgnoreCase(method.getCode())) {
                    return method.getCode();
                }
            }
        }
        return tokenizablePaymentMethods.stream()
            .map(PaymentMethod::getCode)
            .filter(code -> code != null && !code.isBlank())
            .findFirst()
            .orElse(null);
    }

    private String handleMissingAuthenticatedCustomer(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/account/login";
        }
        throw accountServiceUnavailable();
    }

    private <T> ResponseEntity<T> accountApiUnavailable(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    private ResponseStatusException accountServiceUnavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account services are temporarily unavailable. Please try again.");
    }

    private String buildAccountDisplayName(Customer customer) {
        String firstName = trimToNull(customer.getFirstName());
        String lastName = trimToNull(customer.getLastName());
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName + " - " + customer.getEmail();
        }
        if (firstName != null) {
            return firstName + " - " + customer.getEmail();
        }
        if (lastName != null) {
            return lastName + " - " + customer.getEmail();
        }
        return customer.getEmail();
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
