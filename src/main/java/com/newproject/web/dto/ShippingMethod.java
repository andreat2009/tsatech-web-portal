package com.newproject.web.dto;

import java.math.BigDecimal;

public class ShippingMethod {
    private Long id;
    private String code;
    private String label;
    private BigDecimal cost;
    private boolean enabled;
    private int sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
