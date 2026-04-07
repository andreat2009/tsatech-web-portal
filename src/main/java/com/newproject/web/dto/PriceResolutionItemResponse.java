package com.newproject.web.dto;

import java.math.BigDecimal;

public class PriceResolutionItemResponse {
    private Long productId;
    private String variantKey;
    private Integer quantity;
    private BigDecimal amount;
    private BigDecimal compareAtAmount;
    private String currency;
    private String priceListCode;
    private String customerGroupCode;
    private Integer priority;
    private Long sourcePriceId;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getVariantKey() { return variantKey; }
    public void setVariantKey(String variantKey) { this.variantKey = variantKey; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getCompareAtAmount() { return compareAtAmount; }
    public void setCompareAtAmount(BigDecimal compareAtAmount) { this.compareAtAmount = compareAtAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPriceListCode() { return priceListCode; }
    public void setPriceListCode(String priceListCode) { this.priceListCode = priceListCode; }
    public String getCustomerGroupCode() { return customerGroupCode; }
    public void setCustomerGroupCode(String customerGroupCode) { this.customerGroupCode = customerGroupCode; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Long getSourcePriceId() { return sourcePriceId; }
    public void setSourcePriceId(Long sourcePriceId) { this.sourcePriceId = sourcePriceId; }
}
