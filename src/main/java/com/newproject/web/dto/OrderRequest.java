package com.newproject.web.dto;

import java.math.BigDecimal;

public class OrderRequest {
    private Long customerId;
    private String currency;
    private BigDecimal total;
    private String status;
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;
    private String customerPhone;
    private String customerLocale;
    private String orderComment;
    private Boolean guestCheckout;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
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
}
