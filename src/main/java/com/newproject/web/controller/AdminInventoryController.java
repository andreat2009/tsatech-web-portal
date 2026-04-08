package com.newproject.web.controller;

import com.newproject.web.dto.AdminInventoryEntry;
import com.newproject.web.dto.InventoryItem;
import com.newproject.web.dto.InventoryRequest;
import com.newproject.web.dto.Product;
import com.newproject.web.dto.ProductVariant;
import com.newproject.web.service.GatewayClient;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping({"/admin/inventory", "/admin/inventario"})
public class AdminInventoryController {
    private static final DateTimeFormatter CSV_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final GatewayClient gatewayClient;

    public AdminInventoryController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String list(
        @RequestParam(name = "query", required = false) String query,
        @RequestParam(name = "state", defaultValue = "all") String state,
        @RequestParam(name = "adjusted", required = false) String adjusted,
        @RequestParam(name = "released", required = false) String released,
        @RequestParam(name = "error", required = false) String error,
        Model model
    ) {
        List<AdminInventoryEntry> entries = filterEntries(loadEntries(), query, state);
        model.addAttribute("inventoryItems", entries);
        model.addAttribute("query", trimToNull(query));
        model.addAttribute("state", normalizeState(state));
        model.addAttribute("returnTo", buildListReturnTo(query, state));
        model.addAttribute("availableUnits", sum(entries, AdminInventoryEntry::getAvailable));
        model.addAttribute("reservedUnits", sum(entries, AdminInventoryEntry::getReserved));
        model.addAttribute("trackedUnits", sum(entries, AdminInventoryEntry::getTracked));
        model.addAttribute("outOfStockCount", entries.stream().filter(AdminInventoryEntry::isOutOfStock).count());
        model.addAttribute("lowStockCount", entries.stream().filter(AdminInventoryEntry::isLowStock).count());
        model.addAttribute("attentionCount", entries.stream().filter(AdminInventoryEntry::requiresAttention).count());
        model.addAttribute("adjusted", adjusted != null);
        model.addAttribute("released", released != null);
        model.addAttribute("error", error);
        return "admin/inventory";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
        @RequestParam(name = "query", required = false) String query,
        @RequestParam(name = "state", defaultValue = "all") String state
    ) {
        List<AdminInventoryEntry> entries = filterEntries(loadEntries(), query, state);
        String csv = buildCsv(entries);
        String filename = "inventory-" + CSV_TIMESTAMP.format(OffsetDateTime.now()) + ".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{inventoryId}")
    public String detail(
        @PathVariable Long inventoryId,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        @RequestParam(name = "adjusted", required = false) String adjusted,
        @RequestParam(name = "released", required = false) String released,
        @RequestParam(name = "error", required = false) String error,
        Model model
    ) {
        Optional<AdminInventoryEntry> entry = findEntry(inventoryId);
        if (entry.isEmpty()) {
            return redirectWithFlag(returnTo, "error=missing");
        }
        model.addAttribute("inventoryItem", entry.get());
        model.addAttribute("returnTo", sanitizeReturnTo(returnTo));
        model.addAttribute("adjusted", adjusted != null);
        model.addAttribute("released", released != null);
        model.addAttribute("error", error);
        return "admin/inventory-detail";
    }

    @PostMapping("/{inventoryId}/adjust")
    public String adjust(
        @PathVariable Long inventoryId,
        @RequestParam Integer onHand,
        @RequestParam Integer reserved,
        @RequestParam(name = "returnTo", required = false) String returnTo
    ) {
        if (onHand == null || reserved == null || onHand < 0 || reserved < 0) {
            return redirectWithFlag(returnTo, "error=invalid");
        }
        Optional<AdminInventoryEntry> entry = findEntry(inventoryId);
        if (entry.isEmpty()) {
            return redirectWithFlag(returnTo, "error=missing");
        }
        try {
            persist(entry.get(), onHand, reserved);
            return redirectWithFlag(returnTo, "adjusted=1");
        } catch (Exception ex) {
            return redirectWithFlag(returnTo, "error=adjust");
        }
    }

    @PostMapping("/{inventoryId}/release-reserved")
    public String releaseReserved(
        @PathVariable Long inventoryId,
        @RequestParam(name = "returnTo", required = false) String returnTo
    ) {
        Optional<AdminInventoryEntry> entry = findEntry(inventoryId);
        if (entry.isEmpty()) {
            return redirectWithFlag(returnTo, "error=missing");
        }
        AdminInventoryEntry item = entry.get();
        try {
            persist(item, safeInt(item.getAvailable()) + safeInt(item.getReserved()), 0);
            return redirectWithFlag(returnTo, "released=1");
        } catch (Exception ex) {
            return redirectWithFlag(returnTo, "error=release");
        }
    }

    private void persist(AdminInventoryEntry item, int onHand, int reserved) {
        InventoryRequest request = new InventoryRequest();
        request.setProductId(item.getProductId());
        request.setVariantKey(item.getVariantKey());
        request.setOnHand(onHand);
        request.setReserved(reserved);
        if (item.isVariantScoped()) {
            gatewayClient.updateVariantInventory(item.getProductId(), item.getVariantKey(), request);
        } else {
            gatewayClient.updateInventory(item.getProductId(), request);
        }
    }

    private Optional<AdminInventoryEntry> findEntry(Long inventoryId) {
        return loadEntries().stream()
            .filter(entry -> Objects.equals(entry.getInventoryId(), inventoryId))
            .findFirst();
    }

    private List<AdminInventoryEntry> loadEntries() {
        Map<Long, Product> productById = gatewayClient.listProducts().stream()
            .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left, LinkedHashMap::new));

        return gatewayClient.listInventory().stream()
            .map(item -> toEntry(item, productById.get(item.getProductId())))
            .sorted(Comparator
                .comparing(AdminInventoryEntry::requiresAttention).reversed()
                .thenComparing(AdminInventoryEntry::isOutOfStock).reversed()
                .thenComparing(AdminInventoryEntry::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(entry -> normalize(entry.getProductName()))
                .thenComparing(entry -> normalize(entry.getResolvedVariantLabel())))
            .toList();
    }

    private List<AdminInventoryEntry> filterEntries(List<AdminInventoryEntry> entries, String query, String state) {
        String normalizedQuery = normalize(trimToNull(query));
        String normalizedState = normalizeState(state);
        return entries.stream()
            .filter(entry -> matchesQuery(entry, normalizedQuery))
            .filter(entry -> matchesState(entry, normalizedState))
            .toList();
    }

    private boolean matchesQuery(AdminInventoryEntry entry, String normalizedQuery) {
        if (normalizedQuery == null) {
            return true;
        }
        return normalize(entry.getProductName()).contains(normalizedQuery)
            || normalize(entry.getProductSku()).contains(normalizedQuery)
            || normalize(entry.getResolvedVariantLabel()).contains(normalizedQuery)
            || normalize(entry.getVariantKey()).contains(normalizedQuery)
            || normalize(entry.getProductId() != null ? entry.getProductId().toString() : null).contains(normalizedQuery);
    }

    private boolean matchesState(AdminInventoryEntry entry, String state) {
        return switch (state) {
            case "attention" -> entry.requiresAttention();
            case "out" -> entry.isOutOfStock();
            case "low" -> entry.isLowStock();
            case "reserved" -> entry.hasReservedUnits();
            case "variant" -> entry.isVariantScoped();
            case "healthy" -> !entry.requiresAttention();
            default -> true;
        };
    }

    private AdminInventoryEntry toEntry(InventoryItem item, Product product) {
        AdminInventoryEntry entry = new AdminInventoryEntry();
        entry.setInventoryId(item.getId());
        entry.setProductId(item.getProductId());
        entry.setVariantKey(blankToEmpty(item.getVariantKey()));
        entry.setAvailable(safeInt(item.getOnHand()));
        entry.setReserved(safeInt(item.getReserved()));
        entry.setTracked(safeInt(item.getOnHand()) + safeInt(item.getReserved()));
        entry.setUpdatedAt(item.getUpdatedAt());

        if (product != null) {
            entry.setProductName(product.getName());
            entry.setProductSku(product.getSku());
            entry.setProductImageUrl(product.getPrimaryImageUrl());
            entry.setProductAdminPath("/admin/catalogo/prodotti/" + product.getId() + "/modifica");
            entry.setProductActive(product.getActive());
            if (entry.isVariantScoped()) {
                entry.setVariantLabel(resolveVariantLabel(product, entry.getVariantKey()));
            }
        } else {
            entry.setProductName("Product #" + item.getProductId());
            entry.setProductAdminPath("/admin/catalogo/prodotti/" + item.getProductId() + "/modifica");
        }

        return entry;
    }

    private String resolveVariantLabel(Product product, String variantKey) {
        if (product == null || product.getVariants() == null) {
            return variantKey;
        }
        return product.getVariants().stream()
            .filter(variant -> Objects.equals(blankToEmpty(variant.getVariantKey()), blankToEmpty(variantKey)))
            .map(ProductVariant::getResolvedLabel)
            .findFirst()
            .orElse(variantKey);
    }

    private String buildCsv(List<AdminInventoryEntry> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append("inventory_id,product_id,product_name,sku,scope,variant_key,variant_label,available,reserved,tracked,stock_state,updated_at\n");
        for (AdminInventoryEntry entry : entries) {
            builder
                .append(csv(entry.getInventoryId())).append(',')
                .append(csv(entry.getProductId())).append(',')
                .append(csv(entry.getProductName())).append(',')
                .append(csv(entry.getProductSku())).append(',')
                .append(csv(entry.isVariantScoped() ? "VARIANT" : "BASE")).append(',')
                .append(csv(entry.getVariantKey())).append(',')
                .append(csv(entry.getResolvedVariantLabel())).append(',')
                .append(csv(entry.getAvailable())).append(',')
                .append(csv(entry.getReserved())).append(',')
                .append(csv(entry.getTracked())).append(',')
                .append(csv(entry.getStockStateKey())).append(',')
                .append(csv(entry.getUpdatedAt()))
                .append('\n');
        }
        return builder.toString();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\n")) {
            return "\"" + text + "\"";
        }
        return text;
    }

    private String buildListReturnTo(String query, String state) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/inventory");
        String normalizedQuery = trimToNull(query);
        String normalizedState = normalizeState(state);
        if (normalizedQuery != null) {
            builder.queryParam("query", normalizedQuery);
        }
        if (!"all".equals(normalizedState)) {
            builder.queryParam("state", normalizedState);
        }
        return builder.build().encode().toUriString();
    }

    private String sanitizeReturnTo(String returnTo) {
        if (returnTo != null && returnTo.startsWith("/admin/inventory")) {
            return returnTo;
        }
        return "/admin/inventory";
    }

    private String redirectWithFlag(String returnTo, String flag) {
        String base = sanitizeReturnTo(returnTo);
        return "redirect:" + base + (base.contains("?") ? "&" : "?") + flag;
    }

    private long sum(List<AdminInventoryEntry> entries, java.util.function.Function<AdminInventoryEntry, Integer> extractor) {
        return entries.stream()
            .map(extractor)
            .filter(Objects::nonNull)
            .mapToLong(Integer::longValue)
            .sum();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String normalizeState(String state) {
        String normalized = normalize(state);
        if (normalized == null) {
            return "all";
        }
        return switch (normalized) {
            case "attention", "out", "low", "reserved", "variant", "healthy" -> normalized;
            default -> "all";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
