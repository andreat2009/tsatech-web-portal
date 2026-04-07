package com.newproject.web.dto;

import java.math.BigDecimal;

public class OrderItemRequest {
    private Long productId;
    private String variantKey;
    private String variantDisplayName;
    private String sku;
    private String name;
    private Integer quantity;
    private BigDecimal unitPrice;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getVariantKey() { return variantKey; }
    public void setVariantKey(String variantKey) { this.variantKey = variantKey; }
    public String getVariantDisplayName() { return variantDisplayName; }
    public void setVariantDisplayName(String variantDisplayName) { this.variantDisplayName = variantDisplayName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
