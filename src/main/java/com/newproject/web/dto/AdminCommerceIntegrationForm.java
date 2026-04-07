package com.newproject.web.dto;

public class AdminCommerceIntegrationForm {
    private String code;
    private String displayName;
    private String providerType;
    private String syncMode;
    private String authType;
    private String baseUrl;
    private String username;
    private String apiKey;
    private String apiSecret;
    private Boolean clearApiKey;
    private Boolean clearApiSecret;
    private String catalogEndpoint;
    private String inventoryEndpoint;
    private String ordersEndpoint;
    private String customersEndpoint;
    private Boolean syncCatalog;
    private Boolean syncInventory;
    private Boolean syncOrders;
    private Boolean syncCustomers;
    private Boolean active;
    private Boolean apiKeyConfigured;
    private Boolean apiSecretConfigured;
    private Boolean providerConfigurationAvailable;
    private String credentialKeySource;
    private String lastSyncStatus;
    private String lastSyncSummary;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    public Boolean getClearApiKey() { return clearApiKey; }
    public void setClearApiKey(Boolean clearApiKey) { this.clearApiKey = clearApiKey; }
    public Boolean getClearApiSecret() { return clearApiSecret; }
    public void setClearApiSecret(Boolean clearApiSecret) { this.clearApiSecret = clearApiSecret; }
    public String getCatalogEndpoint() { return catalogEndpoint; }
    public void setCatalogEndpoint(String catalogEndpoint) { this.catalogEndpoint = catalogEndpoint; }
    public String getInventoryEndpoint() { return inventoryEndpoint; }
    public void setInventoryEndpoint(String inventoryEndpoint) { this.inventoryEndpoint = inventoryEndpoint; }
    public String getOrdersEndpoint() { return ordersEndpoint; }
    public void setOrdersEndpoint(String ordersEndpoint) { this.ordersEndpoint = ordersEndpoint; }
    public String getCustomersEndpoint() { return customersEndpoint; }
    public void setCustomersEndpoint(String customersEndpoint) { this.customersEndpoint = customersEndpoint; }
    public Boolean getSyncCatalog() { return syncCatalog; }
    public void setSyncCatalog(Boolean syncCatalog) { this.syncCatalog = syncCatalog; }
    public Boolean getSyncInventory() { return syncInventory; }
    public void setSyncInventory(Boolean syncInventory) { this.syncInventory = syncInventory; }
    public Boolean getSyncOrders() { return syncOrders; }
    public void setSyncOrders(Boolean syncOrders) { this.syncOrders = syncOrders; }
    public Boolean getSyncCustomers() { return syncCustomers; }
    public void setSyncCustomers(Boolean syncCustomers) { this.syncCustomers = syncCustomers; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getApiKeyConfigured() { return apiKeyConfigured; }
    public void setApiKeyConfigured(Boolean apiKeyConfigured) { this.apiKeyConfigured = apiKeyConfigured; }
    public Boolean getApiSecretConfigured() { return apiSecretConfigured; }
    public void setApiSecretConfigured(Boolean apiSecretConfigured) { this.apiSecretConfigured = apiSecretConfigured; }
    public Boolean getProviderConfigurationAvailable() { return providerConfigurationAvailable; }
    public void setProviderConfigurationAvailable(Boolean providerConfigurationAvailable) { this.providerConfigurationAvailable = providerConfigurationAvailable; }
    public String getCredentialKeySource() { return credentialKeySource; }
    public void setCredentialKeySource(String credentialKeySource) { this.credentialKeySource = credentialKeySource; }
    public String getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(String lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }
    public String getLastSyncSummary() { return lastSyncSummary; }
    public void setLastSyncSummary(String lastSyncSummary) { this.lastSyncSummary = lastSyncSummary; }
}
