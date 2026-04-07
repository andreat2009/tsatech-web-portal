package com.newproject.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ProductPrice {
    private Long id;
    private Long productId;
    private String variantKey;
    private String priceListCode;
    private String customerGroupCode;
    private BigDecimal compareAtAmount;
    private BigDecimal amount;
    private String currency;
    private Integer priority;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private Boolean active;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getVariantKey() { return variantKey; }
    public void setVariantKey(String variantKey) { this.variantKey = variantKey; }
    public String getPriceListCode() { return priceListCode; }
    public void setPriceListCode(String priceListCode) { this.priceListCode = priceListCode; }
    public String getCustomerGroupCode() { return customerGroupCode; }
    public void setCustomerGroupCode(String customerGroupCode) { this.customerGroupCode = customerGroupCode; }
    public BigDecimal getCompareAtAmount() { return compareAtAmount; }
    public void setCompareAtAmount(BigDecimal compareAtAmount) { this.compareAtAmount = compareAtAmount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime endsAt) { this.endsAt = endsAt; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
