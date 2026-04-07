package com.newproject.web.dto;

import java.util.ArrayList;
import java.util.List;

public class PriceResolutionResponse {
    private String priceListCode;
    private String customerGroupCode;
    private List<PriceResolutionItemResponse> items = new ArrayList<>();

    public String getPriceListCode() { return priceListCode; }
    public void setPriceListCode(String priceListCode) { this.priceListCode = priceListCode; }
    public String getCustomerGroupCode() { return customerGroupCode; }
    public void setCustomerGroupCode(String customerGroupCode) { this.customerGroupCode = customerGroupCode; }
    public List<PriceResolutionItemResponse> getItems() { return items; }
    public void setItems(List<PriceResolutionItemResponse> items) { this.items = items; }
}
