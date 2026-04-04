package com.newproject.web.controller;

import com.newproject.web.dto.AdminPaymentMethodForm;
import com.newproject.web.dto.PaymentMethod;
import com.newproject.web.service.GatewayClient;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/admin/payment-methods", "/admin/pagamenti/metodi"})
public class AdminPaymentMethodController {
    private static final List<String> PROVIDER_OPTIONS = List.of("OFFLINE", "PAYPAL", "FABRICK");
    private static final List<String> PAYMENT_FLOW_OPTIONS = List.of("OFFLINE", "REDIRECT", "LIGHTBOX");
    private static final List<String> ENVIRONMENT_OPTIONS = List.of("sandbox", "production");

    private final GatewayClient gatewayClient;

    public AdminPaymentMethodController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("paymentMethods", gatewayClient.listAdminPaymentMethods());
        return "admin/payment-methods";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        AdminPaymentMethodForm form = new AdminPaymentMethodForm();
        form.setProvider("OFFLINE");
        form.setPaymentFlow("OFFLINE");
        form.setActive(true);
        form.setSortOrder(10);
        form.setProviderConfigurationAvailable(true);
        fillFormModel(model, form, "/admin/payment-methods", "New Payment Method");
        return "admin/payment-method-form";
    }

    @PostMapping
    public String create(@ModelAttribute("paymentMethod") AdminPaymentMethodForm form) {
        gatewayClient.createAdminPaymentMethod(form);
        return "redirect:/admin/payment-methods";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PaymentMethod method = gatewayClient.getAdminPaymentMethod(id);
        fillFormModel(model, toForm(method), "/admin/payment-methods/" + id, "Edit Payment Method & Credentials");
        return "admin/payment-method-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("paymentMethod") AdminPaymentMethodForm form) {
        gatewayClient.updateAdminPaymentMethod(id, form);
        return "redirect:/admin/payment-methods";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gatewayClient.deleteAdminPaymentMethod(id);
        return "redirect:/admin/payment-methods";
    }

    private void fillFormModel(Model model, AdminPaymentMethodForm form, String action, String title) {
        model.addAttribute("paymentMethod", form);
        model.addAttribute("providerOptions", PROVIDER_OPTIONS);
        model.addAttribute("paymentFlowOptions", PAYMENT_FLOW_OPTIONS);
        model.addAttribute("environmentOptions", ENVIRONMENT_OPTIONS);
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
    }

    private AdminPaymentMethodForm toForm(PaymentMethod method) {
        AdminPaymentMethodForm form = new AdminPaymentMethodForm();
        form.setCode(method.getCode());
        form.setDisplayName(method.getDisplayName());
        form.setProvider(method.getProvider());
        form.setPaymentFlow(method.getPaymentFlow());
        form.setDescription(method.getDescription());
        form.setActive(method.getActive() == null || method.getActive());
        form.setSortOrder(method.getSortOrder());
        form.setProviderEnvironment(method.getProviderEnvironment());
        form.setProviderBaseUrl(method.getProviderBaseUrl());
        form.setProviderBrandName(method.getProviderBrandName());
        form.setProviderWebhookId(method.getProviderWebhookId());
        form.setProviderClientId(method.getProviderClientId());
        form.setProviderClientSecretConfigured(Boolean.TRUE.equals(method.getProviderClientSecretConfigured()));
        form.setProviderClientSecretSource(method.getProviderClientSecretSource());
        form.setProviderShopLogin(method.getProviderShopLogin());
        form.setProviderApiKeyConfigured(Boolean.TRUE.equals(method.getProviderApiKeyConfigured()));
        form.setProviderApiKeySource(method.getProviderApiKeySource());
        form.setProviderLightboxScriptUrl(method.getProviderLightboxScriptUrl());
        form.setProviderNotificationUrl(method.getProviderNotificationUrl());
        form.setProviderConfigurationAvailable(method.getProviderConfigurationAvailable() == null || method.getProviderConfigurationAvailable());
        form.setClearProviderClientSecret(false);
        form.setClearProviderApiKey(false);
        return form;
    }
}
