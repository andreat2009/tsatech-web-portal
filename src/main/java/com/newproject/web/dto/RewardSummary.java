package com.newproject.web.dto;

public class RewardSummary {
    private Long customerId;
    private Integer pointsBalance;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Integer getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(Integer pointsBalance) { this.pointsBalance = pointsBalance; }
}
