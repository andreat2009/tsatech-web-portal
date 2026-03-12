package com.newproject.web.dto;

import java.time.OffsetDateTime;

public class StoreSettings {
    private Long id;
    private String siteName;
    private String logoUrl;
    private Integer logoMaxHeightPx;
    private Integer siteNameFontSizePx;
    private String contactEmail;
    private String supportEmail;
    private String supportPhone;
    private String supportPhoneSecondary;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postalCode;
    private String country;
    private Boolean smtpEnabled;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private Boolean smtpAuth;
    private Boolean smtpStarttls;
    private String mailFromEmail;
    private String mailFromName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public Integer getLogoMaxHeightPx() { return logoMaxHeightPx; }
    public void setLogoMaxHeightPx(Integer logoMaxHeightPx) { this.logoMaxHeightPx = logoMaxHeightPx; }
    public Integer getSiteNameFontSizePx() { return siteNameFontSizePx; }
    public void setSiteNameFontSizePx(Integer siteNameFontSizePx) { this.siteNameFontSizePx = siteNameFontSizePx; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }
    public String getSupportPhone() { return supportPhone; }
    public void setSupportPhone(String supportPhone) { this.supportPhone = supportPhone; }
    public String getSupportPhoneSecondary() { return supportPhoneSecondary; }
    public void setSupportPhoneSecondary(String supportPhoneSecondary) { this.supportPhoneSecondary = supportPhoneSecondary; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public Boolean getSmtpEnabled() { return smtpEnabled; }
    public void setSmtpEnabled(Boolean smtpEnabled) { this.smtpEnabled = smtpEnabled; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer smtpPort) { this.smtpPort = smtpPort; }
    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }
    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }
    public Boolean getSmtpAuth() { return smtpAuth; }
    public void setSmtpAuth(Boolean smtpAuth) { this.smtpAuth = smtpAuth; }
    public Boolean getSmtpStarttls() { return smtpStarttls; }
    public void setSmtpStarttls(Boolean smtpStarttls) { this.smtpStarttls = smtpStarttls; }
    public String getMailFromEmail() { return mailFromEmail; }
    public void setMailFromEmail(String mailFromEmail) { this.mailFromEmail = mailFromEmail; }
    public String getMailFromName() { return mailFromName; }
    public void setMailFromName(String mailFromName) { this.mailFromName = mailFromName; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
