package com.newproject.web.dto;

import java.math.BigDecimal;

public class AdminShippingMethodForm {
    private String code;
    private String label;
    private BigDecimal cost;
    private Boolean enabled;
    private Integer sortOrder;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
