package com.newproject.web.controller;

import com.newproject.web.dto.Category;
import com.newproject.web.dto.Product;
import com.newproject.web.dto.ProductRequest;
import com.newproject.web.service.GatewayClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final GatewayClient gatewayClient;

    public AdminProductController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(Model model) {
        List<Product> products = gatewayClient.listProducts();
        model.addAttribute("products", products);
        return "admin/products";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        ProductRequest product = new ProductRequest();
        product.setActive(true);
        product.setCategoryIds(new HashSet<>());
        model.addAttribute("product", product);
        model.addAttribute("categories", gatewayClient.listCategories(true));
        model.addAttribute("formTitle", "Nuovo prodotto");
        model.addAttribute("formAction", "/admin/products");
        return "admin/product-form";
    }

    @PostMapping
    public String create(@ModelAttribute ProductRequest request) {
        normalizeProductRequest(request);
        gatewayClient.createProduct(request);
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = gatewayClient.getProduct(id);
        ProductRequest request = new ProductRequest();
        request.setSku(product.getSku());
        request.setModel(product.getModel());
        request.setName(product.getName());
        request.setDescription(product.getDescription());
        request.setPrice(product.getPrice());
        request.setQuantity(product.getQuantity());
        request.setActive(product.getActive());
        request.setImage(product.getImage());
        request.setManufacturerId(product.getManufacturerId());
        request.setCategoryIds(product.getCategoryIds());

        model.addAttribute("product", request);
        model.addAttribute("categories", gatewayClient.listCategories(true));
        model.addAttribute("formTitle", "Modifica prodotto");
        model.addAttribute("formAction", "/admin/products/" + id);
        return "admin/product-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute ProductRequest request) {
        normalizeProductRequest(request);
        gatewayClient.updateProduct(id, request);
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gatewayClient.deleteProduct(id);
        return "redirect:/admin/products";
    }

    private void normalizeProductRequest(ProductRequest request) {
        if (request.getSku() == null || request.getSku().isBlank()) {
            request.setSku("SKU-" + Instant.now().getEpochSecond());
        }
        if (request.getPrice() == null) {
            request.setPrice(BigDecimal.ZERO);
        }
        if (request.getQuantity() == null) {
            request.setQuantity(0);
        }
        if (request.getActive() == null) {
            request.setActive(true);
        }
        if (request.getCategoryIds() == null) {
            request.setCategoryIds(new HashSet<>());
        }
    }
}
