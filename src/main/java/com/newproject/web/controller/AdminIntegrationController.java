package com.newproject.web.controller;

import com.newproject.web.dto.AdminCommerceIntegrationForm;
import com.newproject.web.dto.CommerceIntegration;
import com.newproject.web.service.GatewayClient;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping({"/admin/integrations", "/admin/system/integrations"})
public class AdminIntegrationController {
    private static final List<String> PROVIDER_OPTIONS = List.of("BROADLEAF", "SHOPIFY", "WOOCOMMERCE", "MAGENTO", "OPENCART", "CUSTOM_REST");
    private static final List<String> AUTH_TYPE_OPTIONS = List.of("NONE", "API_KEY", "BEARER", "BASIC");
    private static final List<String> SYNC_MODE_OPTIONS = List.of("PULL", "PUSH", "BIDIRECTIONAL");

    private final GatewayClient gatewayClient;

    public AdminIntegrationController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(
        @RequestParam(name = "saved", required = false) String saved,
        @RequestParam(name = "updated", required = false) String updated,
        @RequestParam(name = "deleted", required = false) String deleted,
        @RequestParam(name = "error", required = false) String error,
        Model model
    ) {
        model.addAttribute("integrations", gatewayClient.listCommerceIntegrations());
        model.addAttribute("saved", saved != null);
        model.addAttribute("updated", updated != null);
        model.addAttribute("deleted", deleted != null);
        model.addAttribute("error", error);
        return "admin/integrations";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        AdminCommerceIntegrationForm form = new AdminCommerceIntegrationForm();
        form.setProviderType("CUSTOM_REST");
        form.setAuthType("API_KEY");
        form.setSyncMode("BIDIRECTIONAL");
        form.setActive(true);
        form.setSyncCatalog(true);
        form.setSyncInventory(true);
        form.setSyncOrders(true);
        form.setSyncCustomers(false);
        form.setProviderConfigurationAvailable(false);
        fillFormModel(model, form, "/admin/integrations", "New Integration Connector");
        return "admin/integration-form";
    }

    @PostMapping
    public String create(@ModelAttribute("integration") AdminCommerceIntegrationForm form) {
        try {
            gatewayClient.createCommerceIntegration(form);
            return "redirect:/admin/integrations?saved=1";
        } catch (Exception ex) {
            return "redirect:/admin/integrations?error=create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CommerceIntegration integration = gatewayClient.getCommerceIntegration(id);
        fillFormModel(model, toForm(integration), "/admin/integrations/" + id, "Edit Integration Connector");
        return "admin/integration-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("integration") AdminCommerceIntegrationForm form) {
        try {
            gatewayClient.updateCommerceIntegration(id, form);
            return "redirect:/admin/integrations?updated=1";
        } catch (Exception ex) {
            return "redirect:/admin/integrations?error=update";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        try {
            gatewayClient.deleteCommerceIntegration(id);
            return "redirect:/admin/integrations?deleted=1";
        } catch (Exception ex) {
            return "redirect:/admin/integrations?error=delete";
        }
    }

    private void fillFormModel(Model model, AdminCommerceIntegrationForm form, String action, String title) {
        model.addAttribute("integration", form);
        model.addAttribute("providerOptions", PROVIDER_OPTIONS);
        model.addAttribute("authTypeOptions", AUTH_TYPE_OPTIONS);
        model.addAttribute("syncModeOptions", SYNC_MODE_OPTIONS);
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
    }

    private AdminCommerceIntegrationForm toForm(CommerceIntegration integration) {
        AdminCommerceIntegrationForm form = new AdminCommerceIntegrationForm();
        form.setCode(integration.getCode());
        form.setDisplayName(integration.getDisplayName());
        form.setProviderType(integration.getProviderType());
        form.setSyncMode(integration.getSyncMode());
        form.setAuthType(integration.getAuthType());
        form.setBaseUrl(integration.getBaseUrl());
        form.setUsername(integration.getUsername());
        form.setCatalogEndpoint(integration.getCatalogEndpoint());
        form.setInventoryEndpoint(integration.getInventoryEndpoint());
        form.setOrdersEndpoint(integration.getOrdersEndpoint());
        form.setCustomersEndpoint(integration.getCustomersEndpoint());
        form.setSyncCatalog(Boolean.TRUE.equals(integration.getSyncCatalog()));
        form.setSyncInventory(Boolean.TRUE.equals(integration.getSyncInventory()));
        form.setSyncOrders(Boolean.TRUE.equals(integration.getSyncOrders()));
        form.setSyncCustomers(Boolean.TRUE.equals(integration.getSyncCustomers()));
        form.setActive(integration.getActive() == null || integration.getActive());
        form.setApiKeyConfigured(Boolean.TRUE.equals(integration.getApiKeyConfigured()));
        form.setApiSecretConfigured(Boolean.TRUE.equals(integration.getApiSecretConfigured()));
        form.setProviderConfigurationAvailable(Boolean.TRUE.equals(integration.getProviderConfigurationAvailable()));
        form.setCredentialKeySource(integration.getCredentialKeySource());
        form.setLastSyncStatus(integration.getLastSyncStatus());
        form.setLastSyncSummary(integration.getLastSyncSummary());
        form.setClearApiKey(false);
        form.setClearApiSecret(false);
        return form;
    }
}
