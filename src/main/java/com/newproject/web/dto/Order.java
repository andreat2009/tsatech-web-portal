package com.newproject.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class Order {
    private Long id;
    private Long customerId;
    private String currency;
    private BigDecimal total;
    private BigDecimal discountTotal;
    private String customerGroupCode;
    private String appliedCouponCode;
    private String appliedOfferCodes;
    private String status;
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;
    private String customerPhone;
    private String customerLocale;
    private String orderComment;
    private Boolean guestCheckout;
    // SECURITY (H5): presente solo nella create response di un guest order; usato per
    // autorizzare le mutazioni successive (header X-Guest-Order-Token).
    private String guestToken;
    private List<OrderCustomField> customFields;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getDiscountTotal() { return discountTotal; }
    public void setDiscountTotal(BigDecimal discountTotal) { this.discountTotal = discountTotal; }
    public String getCustomerGroupCode() { return customerGroupCode; }
    public void setCustomerGroupCode(String customerGroupCode) { this.customerGroupCode = customerGroupCode; }
    public String getAppliedCouponCode() { return appliedCouponCode; }
    public void setAppliedCouponCode(String appliedCouponCode) { this.appliedCouponCode = appliedCouponCode; }
    public String getAppliedOfferCodes() { return appliedOfferCodes; }
    public void setAppliedOfferCodes(String appliedOfferCodes) { this.appliedOfferCodes = appliedOfferCodes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerFirstName() { return customerFirstName; }
    public void setCustomerFirstName(String customerFirstName) { this.customerFirstName = customerFirstName; }
    public String getCustomerLastName() { return customerLastName; }
    public void setCustomerLastName(String customerLastName) { this.customerLastName = customerLastName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerLocale() { return customerLocale; }
    public void setCustomerLocale(String customerLocale) { this.customerLocale = customerLocale; }
    public String getOrderComment() { return orderComment; }
    public void setOrderComment(String orderComment) { this.orderComment = orderComment; }
    public Boolean getGuestCheckout() { return guestCheckout; }
    public void setGuestCheckout(Boolean guestCheckout) { this.guestCheckout = guestCheckout; }
    public String getGuestToken() { return guestToken; }
    public void setGuestToken(String guestToken) { this.guestToken = guestToken; }
    public List<OrderCustomField> getCustomFields() { return customFields; }
    public void setCustomFields(List<OrderCustomField> customFields) { this.customFields = customFields; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
