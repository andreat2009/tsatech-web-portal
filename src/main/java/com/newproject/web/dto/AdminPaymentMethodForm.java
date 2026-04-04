package com.newproject.web.dto;

public class AdminPaymentMethodForm {
    private String code;
    private String displayName;
    private String provider;
    private String paymentFlow;
    private String description;
    private Boolean active;
    private Integer sortOrder;
    private String providerEnvironment;
    private String providerBaseUrl;
    private String providerBrandName;
    private String providerWebhookId;
    private String providerClientId;
    private String providerClientSecret;
    private Boolean clearProviderClientSecret;
    private String providerClientSecretSource;
    private Boolean providerClientSecretConfigured;
    private String providerShopLogin;
    private String providerApiKey;
    private Boolean clearProviderApiKey;
    private String providerApiKeySource;
    private Boolean providerApiKeyConfigured;
    private String providerLightboxScriptUrl;
    private String providerNotificationUrl;
    private Boolean providerConfigurationAvailable;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPaymentFlow() { return paymentFlow; }
    public void setPaymentFlow(String paymentFlow) { this.paymentFlow = paymentFlow; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getProviderEnvironment() { return providerEnvironment; }
    public void setProviderEnvironment(String providerEnvironment) { this.providerEnvironment = providerEnvironment; }
    public String getProviderBaseUrl() { return providerBaseUrl; }
    public void setProviderBaseUrl(String providerBaseUrl) { this.providerBaseUrl = providerBaseUrl; }
    public String getProviderBrandName() { return providerBrandName; }
    public void setProviderBrandName(String providerBrandName) { this.providerBrandName = providerBrandName; }
    public String getProviderWebhookId() { return providerWebhookId; }
    public void setProviderWebhookId(String providerWebhookId) { this.providerWebhookId = providerWebhookId; }
    public String getProviderClientId() { return providerClientId; }
    public void setProviderClientId(String providerClientId) { this.providerClientId = providerClientId; }
    public String getProviderClientSecret() { return providerClientSecret; }
    public void setProviderClientSecret(String providerClientSecret) { this.providerClientSecret = providerClientSecret; }
    public Boolean getClearProviderClientSecret() { return clearProviderClientSecret; }
    public void setClearProviderClientSecret(Boolean clearProviderClientSecret) { this.clearProviderClientSecret = clearProviderClientSecret; }
    public String getProviderClientSecretSource() { return providerClientSecretSource; }
    public void setProviderClientSecretSource(String providerClientSecretSource) { this.providerClientSecretSource = providerClientSecretSource; }
    public Boolean getProviderClientSecretConfigured() { return providerClientSecretConfigured; }
    public void setProviderClientSecretConfigured(Boolean providerClientSecretConfigured) { this.providerClientSecretConfigured = providerClientSecretConfigured; }
    public String getProviderShopLogin() { return providerShopLogin; }
    public void setProviderShopLogin(String providerShopLogin) { this.providerShopLogin = providerShopLogin; }
    public String getProviderApiKey() { return providerApiKey; }
    public void setProviderApiKey(String providerApiKey) { this.providerApiKey = providerApiKey; }
    public Boolean getClearProviderApiKey() { return clearProviderApiKey; }
    public void setClearProviderApiKey(Boolean clearProviderApiKey) { this.clearProviderApiKey = clearProviderApiKey; }
    public String getProviderApiKeySource() { return providerApiKeySource; }
    public void setProviderApiKeySource(String providerApiKeySource) { this.providerApiKeySource = providerApiKeySource; }
    public Boolean getProviderApiKeyConfigured() { return providerApiKeyConfigured; }
    public void setProviderApiKeyConfigured(Boolean providerApiKeyConfigured) { this.providerApiKeyConfigured = providerApiKeyConfigured; }
    public String getProviderLightboxScriptUrl() { return providerLightboxScriptUrl; }
    public void setProviderLightboxScriptUrl(String providerLightboxScriptUrl) { this.providerLightboxScriptUrl = providerLightboxScriptUrl; }
    public String getProviderNotificationUrl() { return providerNotificationUrl; }
    public void setProviderNotificationUrl(String providerNotificationUrl) { this.providerNotificationUrl = providerNotificationUrl; }
    public Boolean getProviderConfigurationAvailable() { return providerConfigurationAvailable; }
    public void setProviderConfigurationAvailable(Boolean providerConfigurationAvailable) { this.providerConfigurationAvailable = providerConfigurationAvailable; }
}
