package com.newproject.web.controller;

import com.newproject.web.dto.Customer;
import com.newproject.web.dto.InventoryItem;
import com.newproject.web.dto.Manufacturer;
import com.newproject.web.dto.PriceResolutionItemRequest;
import com.newproject.web.dto.PriceResolutionItemResponse;
import com.newproject.web.dto.PriceResolutionRequest;
import com.newproject.web.dto.PriceResolutionResponse;
import com.newproject.web.dto.Product;
import com.newproject.web.dto.ProductVariant;
import com.newproject.web.service.CustomerResolver;
import com.newproject.web.service.GatewayClient;
import jakarta.servlet.http.HttpSession;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping({"/product", "/catalogo"})
public class CatalogExperienceController {
    private static final String COMPARE_SESSION_KEY = "compareProductIds";

    private final GatewayClient gatewayClient;
    private final CustomerResolver customerResolver;
    private final String currency;

    public CatalogExperienceController(
        GatewayClient gatewayClient,
        CustomerResolver customerResolver,
        @Value("${app.currency}") String currency
    ) {
        this.gatewayClient = gatewayClient;
        this.customerResolver = customerResolver;
        this.currency = currency;
    }

    @GetMapping({"/manufacturer", "/produttori"})
    public String byManufacturer(@RequestParam(required = false) Long manufacturerId, Model model, Authentication authentication) {
        List<Manufacturer> manufacturers = gatewayClient.listManufacturers();
        List<Product> products = gatewayClient.listProducts(null, null, true, null, null, "name_asc");
        if (manufacturerId != null) {
            products = products.stream()
                .filter(product -> manufacturerId.equals(product.getManufacturerId()))
                .collect(Collectors.toList());
        }

        applyCatalogState(products, resolveCustomerGroupCode(authentication));

        model.addAttribute("manufacturers", manufacturers);
        model.addAttribute("selectedManufacturerId", manufacturerId);
        model.addAttribute("products", products);
        return "shop/manufacturer";
    }

    @GetMapping({"/special", "/offerte"})
    public String specials(Model model, Authentication authentication) {
        List<Product> products = gatewayClient.listProducts(null, null, true, null, null, "price_asc");
        applyCatalogState(products, resolveCustomerGroupCode(authentication));
        products = products.stream()
            .filter(product -> product.getPrice() != null)
            .sorted(Comparator.comparing(Product::getPrice, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(30)
            .collect(Collectors.toList());

        model.addAttribute("products", products);
        return "shop/special";
    }

    @GetMapping({"/compare", "/confronta"})
    public String compare(HttpSession session, Model model, Authentication authentication) {
        Set<Long> ids = getCompareIds(session);
        List<Product> products = new ArrayList<>();
        for (Long id : ids) {
            gatewayClient.getProductSafe(id).ifPresent(products::add);
        }
        applyCatalogState(products, resolveCustomerGroupCode(authentication));
        model.addAttribute("products", products);
        return "shop/compare";
    }

    private void applyCatalogState(List<Product> products) {
        applyCatalogState(products, defaultCustomerGroupCode());
    }

    private void applyCatalogState(List<Product> products, String customerGroupCode) {
        if (products == null || products.isEmpty()) {
            return;
        }

        Map<String, PriceResolutionItemResponse> resolvedPrices = resolvePriceMap(products, customerGroupCode);
        List<InventoryItem> inventoryItems = gatewayClient.listInventory().stream()
            .filter(item -> item.getProductId() != null)
            .collect(Collectors.toList());

        Map<Long, InventoryItem> inventoryByProductId = inventoryItems.stream()
            .filter(item -> !hasVariantScope(item.getVariantKey()))
            .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Map<Long, Map<String, InventoryItem>> variantInventory = inventoryItems.stream()
            .filter(item -> hasVariantScope(item.getVariantKey()))
            .collect(Collectors.groupingBy(
                InventoryItem::getProductId,
                LinkedHashMap::new,
                Collectors.toMap(item -> normalizeVariantKey(item.getVariantKey()), Function.identity(), (first, ignored) -> first, LinkedHashMap::new)
            ));

        for (Product product : products) {
            PriceResolutionItemResponse price = resolvedPrices.get(priceResolutionKey(product.getId(), null));
            if (price != null && price.getAmount() != null) {
                product.setPrice(price.getAmount());
            }

            InventoryItem inventory = inventoryByProductId.get(product.getId());
            if (inventory != null && inventory.getOnHand() != null) {
                product.setQuantity(Math.max(0, inventory.getOnHand()));
            }

            int variantQuantity = 0;
            boolean hasActiveVariants = false;
            for (ProductVariant variant : product.getVariants() != null ? product.getVariants() : List.<ProductVariant>of()) {
                if (variant == null || !Boolean.TRUE.equals(variant.getActive()) || !hasVariantScope(variant.getVariantKey())) {
                    continue;
                }
                hasActiveVariants = true;
                String variantKey = normalizeVariantKey(variant.getVariantKey());
                PriceResolutionItemResponse variantPrice = resolvedPrices.get(priceResolutionKey(product.getId(), variantKey));
                if (variantPrice != null && variantPrice.getAmount() != null) {
                    variant.setPriceOverride(variantPrice.getAmount());
                }
                InventoryItem variantStock = variantInventory.getOrDefault(product.getId(), Map.of()).get(variantKey);
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

    private Map<String, PriceResolutionItemResponse> resolvePriceMap(List<Product> products, String customerGroupCode) {
        PriceResolutionRequest request = new PriceResolutionRequest();
        request.setCurrency(currency);
        request.setCustomerGroupCode(normalizeCustomerGroupCode(customerGroupCode));
        request.setAt(OffsetDateTime.now());
        request.setItems(buildPriceResolutionItems(products));
        if (request.getItems().isEmpty()) {
            return Map.of();
        }

        PriceResolutionResponse response = gatewayClient.resolvePrices(request);
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            return Map.of();
        }

        return response.getItems().stream()
            .filter(item -> item.getProductId() != null)
            .collect(Collectors.toMap(
                item -> priceResolutionKey(item.getProductId(), item.getVariantKey()),
                Function.identity(),
                (first, ignored) -> first,
                LinkedHashMap::new
            ));
    }

    private List<PriceResolutionItemRequest> buildPriceResolutionItems(List<Product> products) {
        List<PriceResolutionItemRequest> items = new ArrayList<>();
        for (Product product : products) {
            if (product == null || product.getId() == null) {
                continue;
            }
            PriceResolutionItemRequest baseItem = new PriceResolutionItemRequest();
            baseItem.setProductId(product.getId());
            baseItem.setQuantity(1);
            items.add(baseItem);

            for (ProductVariant variant : product.getVariants() != null ? product.getVariants() : List.<ProductVariant>of()) {
                if (variant == null || !Boolean.TRUE.equals(variant.getActive()) || !hasVariantScope(variant.getVariantKey())) {
                    continue;
                }
                PriceResolutionItemRequest variantItem = new PriceResolutionItemRequest();
                variantItem.setProductId(product.getId());
                variantItem.setVariantKey(normalizeVariantKey(variant.getVariantKey()));
                variantItem.setQuantity(1);
                items.add(variantItem);
            }
        }
        return items;
    }

    private String resolveCustomerGroupCode(Authentication authentication) {
        Customer customer = customerResolver.resolveCurrentCustomer(authentication);
        return normalizeCustomerGroupCode(customer != null ? customer.getCustomerGroupCode() : null);
    }

    private String normalizeCustomerGroupCode(String customerGroupCode) {
        if (customerGroupCode == null || customerGroupCode.isBlank()) {
            return defaultCustomerGroupCode();
        }
        return customerGroupCode.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String defaultCustomerGroupCode() {
        return "RETAIL";
    }

    private String priceResolutionKey(Long productId, String variantKey) {
        return productId + "::" + (normalizeVariantKey(variantKey) != null ? normalizeVariantKey(variantKey) : "");
    }

    private String normalizeVariantKey(String variantKey) {
        if (variantKey == null || variantKey.isBlank()) {
            return null;
        }
        return variantKey.trim();
    }

    private boolean hasVariantScope(String variantKey) {
        return normalizeVariantKey(variantKey) != null;
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
