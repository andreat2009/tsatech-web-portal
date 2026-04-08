package com.newproject.web.dto;

import java.time.OffsetDateTime;

public class AdminInventoryEntry {
    private Long inventoryId;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private String productAdminPath;
    private Boolean productActive;
    private String variantKey;
    private String variantLabel;
    private Integer available;
    private Integer reserved;
    private Integer tracked;
    private OffsetDateTime updatedAt;

    public Long getInventoryId() { return inventoryId; }
    public void setInventoryId(Long inventoryId) { this.inventoryId = inventoryId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }
    public String getProductAdminPath() { return productAdminPath; }
    public void setProductAdminPath(String productAdminPath) { this.productAdminPath = productAdminPath; }
    public Boolean getProductActive() { return productActive; }
    public void setProductActive(Boolean productActive) { this.productActive = productActive; }
    public String getVariantKey() { return variantKey; }
    public void setVariantKey(String variantKey) { this.variantKey = variantKey; }
    public String getVariantLabel() { return variantLabel; }
    public void setVariantLabel(String variantLabel) { this.variantLabel = variantLabel; }
    public Integer getAvailable() { return available; }
    public void setAvailable(Integer available) { this.available = available; }
    public Integer getReserved() { return reserved; }
    public void setReserved(Integer reserved) { this.reserved = reserved; }
    public Integer getTracked() { return tracked; }
    public void setTracked(Integer tracked) { this.tracked = tracked; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isVariantScoped() { return variantKey != null && !variantKey.isBlank(); }
    public boolean isOutOfStock() { return safeInt(available) <= 0; }
    public boolean isLowStock() {
        int availableUnits = safeInt(available);
        return availableUnits > 0 && availableUnits <= 5;
    }
    public boolean hasReservedUnits() { return safeInt(reserved) > 0; }
    public boolean requiresAttention() { return isOutOfStock() || isLowStock() || hasReservedUnits(); }

    public String getStockStateKey() {
        if (isOutOfStock()) {
            return "admin.inventory.state.out";
        }
        if (isLowStock()) {
            return "admin.inventory.state.low";
        }
        if (hasReservedUnits()) {
            return "admin.inventory.state.reserved";
        }
        return "admin.inventory.state.healthy";
    }

    public String getStockStateClass() {
        if (isOutOfStock()) {
            return "stock-badge danger";
        }
        if (isLowStock()) {
            return "stock-badge warning";
        }
        if (hasReservedUnits()) {
            return "stock-badge info";
        }
        return "stock-badge success";
    }

    public String getScopeKey() {
        return isVariantScoped() ? "admin.inventory.scope.variant" : "admin.inventory.scope.base";
    }

    public String getResolvedVariantLabel() {
        if (variantLabel != null && !variantLabel.isBlank()) {
            return variantLabel;
        }
        if (variantKey != null && !variantKey.isBlank()) {
            return variantKey;
        }
        return "";
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
