package com.newproject.web.dto;

public class AccountProfileForm {
    private String firstName;
    private String lastName;
    private String phone;
    private String preferredPaymentMethodCode;
    private String preferredShippingMethodCode;
    private String shippingLine1;
    private String shippingLine2;
    private String shippingCity;
    private String shippingRegion;
    private String shippingCountry;
    private String shippingPostalCode;
    private String billingLine1;
    private String billingLine2;
    private String billingCity;
    private String billingRegion;
    private String billingCountry;
    private String billingPostalCode;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPreferredPaymentMethodCode() { return preferredPaymentMethodCode; }
    public void setPreferredPaymentMethodCode(String preferredPaymentMethodCode) { this.preferredPaymentMethodCode = preferredPaymentMethodCode; }
    public String getPreferredShippingMethodCode() { return preferredShippingMethodCode; }
    public void setPreferredShippingMethodCode(String preferredShippingMethodCode) { this.preferredShippingMethodCode = preferredShippingMethodCode; }
    public String getShippingLine1() { return shippingLine1; }
    public void setShippingLine1(String shippingLine1) { this.shippingLine1 = shippingLine1; }
    public String getShippingLine2() { return shippingLine2; }
    public void setShippingLine2(String shippingLine2) { this.shippingLine2 = shippingLine2; }
    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }
    public String getShippingRegion() { return shippingRegion; }
    public void setShippingRegion(String shippingRegion) { this.shippingRegion = shippingRegion; }
    public String getShippingCountry() { return shippingCountry; }
    public void setShippingCountry(String shippingCountry) { this.shippingCountry = shippingCountry; }
    public String getShippingPostalCode() { return shippingPostalCode; }
    public void setShippingPostalCode(String shippingPostalCode) { this.shippingPostalCode = shippingPostalCode; }
    public String getBillingLine1() { return billingLine1; }
    public void setBillingLine1(String billingLine1) { this.billingLine1 = billingLine1; }
    public String getBillingLine2() { return billingLine2; }
    public void setBillingLine2(String billingLine2) { this.billingLine2 = billingLine2; }
    public String getBillingCity() { return billingCity; }
    public void setBillingCity(String billingCity) { this.billingCity = billingCity; }
    public String getBillingRegion() { return billingRegion; }
    public void setBillingRegion(String billingRegion) { this.billingRegion = billingRegion; }
    public String getBillingCountry() { return billingCountry; }
    public void setBillingCountry(String billingCountry) { this.billingCountry = billingCountry; }
    public String getBillingPostalCode() { return billingPostalCode; }
    public void setBillingPostalCode(String billingPostalCode) { this.billingPostalCode = billingPostalCode; }
}
