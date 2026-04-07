package com.newproject.web.dto;

import java.math.BigDecimal;

public class AppliedOffer {
    private Long id;
    private String code;
    private String name;
    private String offerScope;
    private Boolean automatic;
    private BigDecimal discount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOfferScope() { return offerScope; }
    public void setOfferScope(String offerScope) { this.offerScope = offerScope; }
    public Boolean getAutomatic() { return automatic; }
    public void setAutomatic(Boolean automatic) { this.automatic = automatic; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
}
