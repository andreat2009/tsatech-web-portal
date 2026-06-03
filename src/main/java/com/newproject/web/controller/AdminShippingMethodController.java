package com.newproject.web.controller;

import com.newproject.web.dto.AdminShippingMethodForm;
import com.newproject.web.dto.ShippingMethod;
import com.newproject.web.service.GatewayClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping({"/admin/shipping-methods", "/admin/spedizioni/metodi"})
public class AdminShippingMethodController {
    private final GatewayClient gatewayClient;

    public AdminShippingMethodController(GatewayClient gatewayClient) {
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
        model.addAttribute("shippingMethods", gatewayClient.listAdminShippingMethods());
        model.addAttribute("saved", saved != null);
        model.addAttribute("updated", updated != null);
        model.addAttribute("deleted", deleted != null);
        model.addAttribute("error", error);
        return "admin/shipping-methods";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        AdminShippingMethodForm form = new AdminShippingMethodForm();
        form.setEnabled(true);
        form.setSortOrder(10);
        fillFormModel(model, form, "/admin/shipping-methods", "New shipping method");
        return "admin/shipping-method-form";
    }

    @PostMapping
    public String create(@ModelAttribute("shippingMethod") AdminShippingMethodForm form) {
        try {
            gatewayClient.createAdminShippingMethod(form);
            return "redirect:/admin/shipping-methods?saved=1";
        } catch (Exception ex) {
            return "redirect:/admin/shipping-methods?error=create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ShippingMethod method = gatewayClient.getAdminShippingMethod(id);
        fillFormModel(model, toForm(method), "/admin/shipping-methods/" + id, "Edit shipping method");
        return "admin/shipping-method-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("shippingMethod") AdminShippingMethodForm form) {
        try {
            gatewayClient.updateAdminShippingMethod(id, form);
            return "redirect:/admin/shipping-methods?updated=1";
        } catch (Exception ex) {
            return "redirect:/admin/shipping-methods?error=update";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        try {
            gatewayClient.deleteAdminShippingMethod(id);
            return "redirect:/admin/shipping-methods?deleted=1";
        } catch (Exception ex) {
            return "redirect:/admin/shipping-methods?error=delete";
        }
    }

    private void fillFormModel(Model model, AdminShippingMethodForm form, String action, String title) {
        model.addAttribute("shippingMethod", form);
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
    }

    private AdminShippingMethodForm toForm(ShippingMethod method) {
        AdminShippingMethodForm form = new AdminShippingMethodForm();
        form.setCode(method.getCode());
        form.setLabel(method.getLabel());
        form.setCost(method.getCost());
        form.setEnabled(method.isEnabled());
        form.setSortOrder(method.getSortOrder());
        return form;
    }
}
