package com.newproject.web.controller;

import com.newproject.web.dto.Manufacturer;
import com.newproject.web.dto.Product;
import com.newproject.web.service.GatewayClient;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/product")
public class CatalogExperienceController {
    private static final String COMPARE_SESSION_KEY = "compareProductIds";

    private final GatewayClient gatewayClient;

    public CatalogExperienceController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping("/manufacturer")
    public String byManufacturer(@RequestParam(required = false) Long manufacturerId, Model model) {
        List<Manufacturer> manufacturers = gatewayClient.listManufacturers();
        List<Product> products = gatewayClient.listProducts(null, null, true, null, null, "name_asc");
        if (manufacturerId != null) {
            products = products.stream()
                .filter(product -> manufacturerId.equals(product.getManufacturerId()))
                .collect(Collectors.toList());
        }

        model.addAttribute("manufacturers", manufacturers);
        model.addAttribute("selectedManufacturerId", manufacturerId);
        model.addAttribute("products", products);
        return "shop/manufacturer";
    }

    @GetMapping("/special")
    public String specials(Model model) {
        List<Product> products = gatewayClient.listProducts(null, null, true, null, null, "price_asc").stream()
            .filter(product -> product.getPrice() != null)
            .limit(30)
            .collect(Collectors.toList());

        model.addAttribute("products", products);
        return "shop/special";
    }

    @GetMapping("/compare")
    public String compare(HttpSession session, Model model) {
        Set<Long> ids = getCompareIds(session);
        List<Product> products = new ArrayList<>();
        for (Long id : ids) {
            gatewayClient.getProductSafe(id).ifPresent(products::add);
        }
        model.addAttribute("products", products);
        return "shop/compare";
    }

    @PostMapping("/compare/add")
    public String compareAdd(@RequestParam Long productId, HttpSession session) {
        Set<Long> ids = getCompareIds(session);
        ids.add(productId);
        session.setAttribute(COMPARE_SESSION_KEY, ids);
        return "redirect:/product/compare";
    }

    @PostMapping("/compare/{productId}/remove")
    public String compareRemove(@PathVariable Long productId, HttpSession session) {
        Set<Long> ids = getCompareIds(session);
        ids.remove(productId);
        session.setAttribute(COMPARE_SESSION_KEY, ids);
        return "redirect:/product/compare";
    }

    @SuppressWarnings("unchecked")
    private Set<Long> getCompareIds(HttpSession session) {
        Object raw = session.getAttribute(COMPARE_SESSION_KEY);
        if (raw instanceof Set<?> set) {
            return ((Set<Long>) set);
        }
        Set<Long> ids = new LinkedHashSet<>();
        session.setAttribute(COMPARE_SESSION_KEY, ids);
        return ids;
    }
}
