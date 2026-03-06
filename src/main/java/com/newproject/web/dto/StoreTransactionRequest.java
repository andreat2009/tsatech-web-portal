package com.newproject.web.dto;

import java.math.BigDecimal;

public class StoreTransactionRequest {
    private Long orderId;
    private String description;
    private BigDecimal amount;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
