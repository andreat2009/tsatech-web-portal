package com.newproject.web.dto;

public class CheckoutForm {
    private Long addressId;
    private String shippingMethod;
    private String paymentMethod;
    private String couponCode;
    private String comment;

    private String guestEmail;
    private String guestFirstName;
    private String guestLastName;
    private String guestPhone;
    private String guestAddressLine1;
    private String guestAddressLine2;
    private String guestCity;
    private String guestRegion;
    private String guestCountry;
    private String guestPostalCode;

    private Boolean createAccount;
    private Boolean newsletter;

    private Boolean useNewAddress;
    private String newAddressLine1;
    private String newAddressLine2;
    private String newCity;
    private String newRegion;
    private String newCountry;
    private String newPostalCode;

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestFirstName() {
        return guestFirstName;
    }

    public void setGuestFirstName(String guestFirstName) {
        this.guestFirstName = guestFirstName;
    }

    public String getGuestLastName() {
        return guestLastName;
    }

    public void setGuestLastName(String guestLastName) {
        this.guestLastName = guestLastName;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public String getGuestAddressLine1() {
        return guestAddressLine1;
    }

    public void setGuestAddressLine1(String guestAddressLine1) {
        this.guestAddressLine1 = guestAddressLine1;
    }

    public String getGuestAddressLine2() {
        return guestAddressLine2;
    }

    public void setGuestAddressLine2(String guestAddressLine2) {
        this.guestAddressLine2 = guestAddressLine2;
    }

    public String getGuestCity() {
        return guestCity;
    }

    public void setGuestCity(String guestCity) {
        this.guestCity = guestCity;
    }

    public String getGuestRegion() {
        return guestRegion;
    }

    public void setGuestRegion(String guestRegion) {
        this.guestRegion = guestRegion;
    }

    public String getGuestCountry() {
        return guestCountry;
    }

    public void setGuestCountry(String guestCountry) {
        this.guestCountry = guestCountry;
    }

    public String getGuestPostalCode() {
        return guestPostalCode;
    }

    public void setGuestPostalCode(String guestPostalCode) {
        this.guestPostalCode = guestPostalCode;
    }

    public Boolean getCreateAccount() {
        return createAccount;
    }

    public void setCreateAccount(Boolean createAccount) {
        this.createAccount = createAccount;
    }

    public Boolean getNewsletter() {
        return newsletter;
    }

    public void setNewsletter(Boolean newsletter) {
        this.newsletter = newsletter;
    }

    public Boolean getUseNewAddress() {
        return useNewAddress;
    }

    public void setUseNewAddress(Boolean useNewAddress) {
        this.useNewAddress = useNewAddress;
    }

    public String getNewAddressLine1() {
        return newAddressLine1;
    }

    public void setNewAddressLine1(String newAddressLine1) {
        this.newAddressLine1 = newAddressLine1;
    }

    public String getNewAddressLine2() {
        return newAddressLine2;
    }

    public void setNewAddressLine2(String newAddressLine2) {
        this.newAddressLine2 = newAddressLine2;
    }

    public String getNewCity() {
        return newCity;
    }

    public void setNewCity(String newCity) {
        this.newCity = newCity;
    }

    public String getNewRegion() {
        return newRegion;
    }

    public void setNewRegion(String newRegion) {
        this.newRegion = newRegion;
    }

    public String getNewCountry() {
        return newCountry;
    }

    public void setNewCountry(String newCountry) {
        this.newCountry = newCountry;
    }

    public String getNewPostalCode() {
        return newPostalCode;
    }

    public void setNewPostalCode(String newPostalCode) {
        this.newPostalCode = newPostalCode;
    }
}
