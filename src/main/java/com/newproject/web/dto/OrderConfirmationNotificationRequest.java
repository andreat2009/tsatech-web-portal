package com.newproject.web.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderConfirmationNotificationRequest {
    private Long orderId;
    private String customerEmail;
    private String customerName;
    private String locale;
    private String currency;
    private BigDecimal total;
    private String storeUrl;
    private List<OrderConfirmationItem> items;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getStoreUrl() {
        return storeUrl;
    }

    public void setStoreUrl(String storeUrl) {
        this.storeUrl = storeUrl;
    }

    public List<OrderConfirmationItem> getItems() {
        return items;
    }

    public void setItems(List<OrderConfirmationItem> items) {
        this.items = items;
    }
}
