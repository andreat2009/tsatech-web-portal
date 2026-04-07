package com.newproject.web.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class PriceResolutionRequest {
    private String priceListCode;
    private String customerGroupCode;
    private String currency;
    private OffsetDateTime at;
    private List<PriceResolutionItemRequest> items = new ArrayList<>();

    public String getPriceListCode() { return priceListCode; }
    public void setPriceListCode(String priceListCode) { this.priceListCode = priceListCode; }
    public String getCustomerGroupCode() { return customerGroupCode; }
    public void setCustomerGroupCode(String customerGroupCode) { this.customerGroupCode = customerGroupCode; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public OffsetDateTime getAt() { return at; }
    public void setAt(OffsetDateTime at) { this.at = at; }
    public List<PriceResolutionItemRequest> getItems() { return items; }
    public void setItems(List<PriceResolutionItemRequest> items) { this.items = items; }
}
