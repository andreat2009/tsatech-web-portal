package com.newproject.web.dto;

import java.time.OffsetDateTime;

public class RewardTransaction {
    private Long id;
    private Long customerId;
    private Long orderId;
    private String description;
    private Integer pointsDelta;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPointsDelta() { return pointsDelta; }
    public void setPointsDelta(Integer pointsDelta) { this.pointsDelta = pointsDelta; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
