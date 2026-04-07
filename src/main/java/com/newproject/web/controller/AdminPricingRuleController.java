package com.newproject.web.controller;

import com.newproject.web.dto.Product;
import com.newproject.web.dto.ProductPrice;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/pricing/rules")
public class AdminPricingRuleController {
    private final GatewayClient gatewayClient;

    public AdminPricingRuleController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        ProductPrice price = new ProductPrice();
        price.setPriceListCode("DEFAULT");
        price.setCurrency("EUR");
        price.setPriority(0);
        price.setActive(true);
        model.addAttribute("price", price);
        model.addAttribute("products", gatewayClient.listProducts());
        model.addAttribute("formTitle", "New pricing rule");
        model.addAttribute("formAction", "/admin/pricing/rules");
        return "admin/pricing-rule-form";
    }

    @PostMapping
    public String create(@ModelAttribute("price") ProductPrice price) {
        normalize(price);
        gatewayClient.createPrice(price);
        return "redirect:/admin/pricing";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ProductPrice price = gatewayClient.getPriceRuleSafe(id).orElse(null);
        if (price == null) {
            return "redirect:/admin/pricing";
        }
        model.addAttribute("price", price);
        model.addAttribute("products", gatewayClient.listProducts());
        model.addAttribute("formTitle", "Edit pricing rule");
        model.addAttribute("formAction", "/admin/pricing/rules/" + id);
        return "admin/pricing-rule-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("price") ProductPrice price) {
        normalize(price);
        gatewayClient.updatePriceRule(id, price);
        return "redirect:/admin/pricing";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gatewayClient.deletePriceRule(id);
        return "redirect:/admin/pricing";
    }

    private void normalize(ProductPrice price) {
        if (price.getCurrency() == null || price.getCurrency().isBlank()) {
            price.setCurrency("EUR");
        }
        if (price.getPriceListCode() == null || price.getPriceListCode().isBlank()) {
            price.setPriceListCode("DEFAULT");
        }
        if (price.getPriority() == null) {
            price.setPriority(0);
        }
        if (price.getActive() == null) {
            price.setActive(true);
        }
        if (price.getVariantKey() != null && price.getVariantKey().isBlank()) {
            price.setVariantKey(null);
        }
        if (price.getCustomerGroupCode() != null && price.getCustomerGroupCode().isBlank()) {
            price.setCustomerGroupCode(null);
        }
        if (price.getAmount() == null) {
            price.setAmount(BigDecimal.ZERO);
        }
    }
}
