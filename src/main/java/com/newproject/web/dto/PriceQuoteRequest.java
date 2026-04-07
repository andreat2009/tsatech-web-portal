package com.newproject.web.dto;

import java.math.BigDecimal;

public class PriceQuoteRequest {
    private BigDecimal subtotal;
    private BigDecimal shipping;
    private String couponCode;
    private String customerGroupCode;
    private Integer itemCount;

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getShipping() { return shipping; }
    public void setShipping(BigDecimal shipping) { this.shipping = shipping; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getCustomerGroupCode() { return customerGroupCode; }
    public void setCustomerGroupCode(String customerGroupCode) { this.customerGroupCode = customerGroupCode; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
}
