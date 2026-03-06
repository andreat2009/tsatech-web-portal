package com.newproject.web.dto;

import java.time.OffsetDateTime;

public class NewsletterPreference {
    private Long customerId;
    private Boolean subscribed;
    private OffsetDateTime updatedAt;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Boolean getSubscribed() { return subscribed; }
    public void setSubscribed(Boolean subscribed) { this.subscribed = subscribed; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
