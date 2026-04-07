package com.newproject.web.dto;

import java.time.OffsetDateTime;

public class CommerceIntegration {
    private Long id;
    private String code;
    private String displayName;
    private String providerType;
    private String syncMode;
    private String authType;
    private String baseUrl;
    private String username;
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
    private String syncSummary;
    private OffsetDateTime lastSyncAt;
    private String lastSyncStatus;
    private String lastSyncSummary;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getSyncSummary() { return syncSummary; }
    public void setSyncSummary(String syncSummary) { this.syncSummary = syncSummary; }
    public OffsetDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(OffsetDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public String getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(String lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }
    public String getLastSyncSummary() { return lastSyncSummary; }
    public void setLastSyncSummary(String lastSyncSummary) { this.lastSyncSummary = lastSyncSummary; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
