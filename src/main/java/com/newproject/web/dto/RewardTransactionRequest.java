package com.newproject.web.dto;

public class RewardTransactionRequest {
    private Long orderId;
    private String description;
    private Integer pointsDelta;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPointsDelta() { return pointsDelta; }
    public void setPointsDelta(Integer pointsDelta) { this.pointsDelta = pointsDelta; }
}
