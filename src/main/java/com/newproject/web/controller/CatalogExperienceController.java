package com.newproject.web.controller;

import com.newproject.web.dto.InventoryItem;
import com.newproject.web.dto.Manufacturer;
import com.newproject.web.dto.Product;
import com.newproject.web.dto.ProductPrice;
import com.newproject.web.dto.ProductVariant;
import com.newproject.web.service.GatewayClient;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping({"/product", "/catalogo"})
public class CatalogExperienceController {
    private static final String COMPARE_SESSION_KEY = "compareProductIds";

    private final GatewayClient gatewayClient;

    public CatalogExperienceController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping({"/manufacturer", "/produttori"})
    public String byManufacturer(@RequestParam(required = false) Long manufacturerId, Model model) {
        List<Manufacturer> manufacturers = gatewayClient.listManufacturers();
        List<Product> products = gatewayClient.listProducts(null, null, true, null, null, "name_asc");
        if (manufacturerId != null) {
            products = products.stream()
                .filter(product -> manufacturerId.equals(product.getManufacturerId()))
                .collect(Collectors.toList());
        }

        applyCatalogState(products);

        model.addAttribute("manufacturers", manufacturers);
        model.addAttribute("selectedManufacturerId", manufacturerId);
        model.addAttribute("products", products);
        return "shop/manufacturer";
    }

    @GetMapping({"/special", "/offerte"})
    public String specials(Model model) {
        List<Product> products = gatewayClient.listProducts(null, null, true, null, null, "price_asc").stream()
            .filter(product -> product.getPrice() != null)
            .limit(30)
            .collect(Collectors.toList());

        applyCatalogState(products);

        model.addAttribute("products", products);
        return "shop/special";
    }

    @GetMapping({"/compare", "/confronta"})
    public String compare(HttpSession session, Model model) {
        Set<Long> ids = getCompareIds(session);
        List<Product> products = new ArrayList<>();
        for (Long id : ids) {
            gatewayClient.getProductSafe(id).ifPresent(products::add);
        }
        applyCatalogState(products);
        model.addAttribute("products", products);
        return "shop/compare";
    }

    private void applyCatalogState(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        List<ProductPrice> prices = gatewayClient.listPrices().stream()
            .filter(price -> price.getProductId() != null)
            .collect(Collectors.toList());
        List<InventoryItem> inventoryItems = gatewayClient.listInventory().stream()
            .filter(item -> item.getProductId() != null)
            .collect(Collectors.toList());

        Map<Long, ProductPrice> pricesByProductId = prices.stream()
            .filter(price -> price.getVariantKey() == null || price.getVariantKey().isBlank())
            .collect(Collectors.toMap(ProductPrice::getProductId, Function.identity(), (first, ignored) -> first));
        Map<Long, InventoryItem> inventoryByProductId = inventoryItems.stream()
            .filter(item -> item.getVariantKey() == null || item.getVariantKey().isBlank())
            .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity(), (first, ignored) -> first));
        Map<Long, Map<String, ProductPrice>> variantPrices = prices.stream()
            .filter(price -> price.getVariantKey() != null && !price.getVariantKey().isBlank())
            .collect(Collectors.groupingBy(
                ProductPrice::getProductId,
                Collectors.toMap(ProductPrice::getVariantKey, Function.identity(), (first, ignored) -> first)
            ));
        Map<Long, Map<String, InventoryItem>> variantInventory = inventoryItems.stream()
            .filter(item -> item.getVariantKey() != null && !item.getVariantKey().isBlank())
            .collect(Collectors.groupingBy(
                InventoryItem::getProductId,
                Collectors.toMap(InventoryItem::getVariantKey, Function.identity(), (first, ignored) -> first)
            ));

        for (Product product : products) {
            ProductPrice price = pricesByProductId.get(product.getId());
            if (price != null && Boolean.TRUE.equals(price.getActive()) && price.getAmount() != null) {
                product.setPrice(price.getAmount());
            }

            InventoryItem inventory = inventoryByProductId.get(product.getId());
            if (inventory != null && inventory.getOnHand() != null) {
                product.setQuantity(Math.max(0, inventory.getOnHand()));
            }

            int variantQuantity = 0;
            boolean hasActiveVariants = false;
            for (ProductVariant variant : product.getVariants() != null ? product.getVariants() : List.<ProductVariant>of()) {
                if (variant == null || !Boolean.TRUE.equals(variant.getActive()) || variant.getVariantKey() == null || variant.getVariantKey().isBlank()) {
                    continue;
                }
                hasActiveVariants = true;
                ProductPrice variantPrice = variantPrices.getOrDefault(product.getId(), Map.of()).get(variant.getVariantKey());
                if (variantPrice != null && Boolean.TRUE.equals(variantPrice.getActive()) && variantPrice.getAmount() != null) {
                    variant.setPriceOverride(variantPrice.getAmount());
                }
                InventoryItem variantStock = variantInventory.getOrDefault(product.getId(), Map.of()).get(variant.getVariantKey());
                if (variantStock != null && variantStock.getOnHand() != null) {
                    variant.setQuantity(Math.max(0, variantStock.getOnHand()));
                }
                if (variant.getQuantity() != null && variant.getQuantity() > 0) {
                    variantQuantity += variant.getQuantity();
                }
            }
            if (hasActiveVariants) {
                product.setQuantity(variantQuantity);
            }
        }
    }

    @PostMapping({"/compare/add", "/confronta/aggiungi"})
    public String compareAdd(@RequestParam Long productId, HttpSession session) {
        Set<Long> ids = getCompareIds(session);
        ids.add(productId);
        session.setAttribute(COMPARE_SESSION_KEY, ids);
        return "redirect:/catalogo/confronta";
    }

    @PostMapping({"/compare/{productId}/remove", "/confronta/{productId}/rimuovi"})
    public String compareRemove(@PathVariable Long productId, HttpSession session) {
        Set<Long> ids = getCompareIds(session);
        ids.remove(productId);
        session.setAttribute(COMPARE_SESSION_KEY, ids);
        return "redirect:/catalogo/confronta";
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
