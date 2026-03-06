package com.newproject.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class CustomerSubscriptionRequest {
    private String planName;
    private String status;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime nextBillingAt;

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public OffsetDateTime getNextBillingAt() { return nextBillingAt; }
    public void setNextBillingAt(OffsetDateTime nextBillingAt) { this.nextBillingAt = nextBillingAt; }
}
