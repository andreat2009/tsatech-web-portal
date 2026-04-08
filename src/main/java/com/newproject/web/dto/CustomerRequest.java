package com.newproject.web.dto;

import java.time.OffsetDateTime;

public class CustomerRequest {
    private String keycloakUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String customerGroupCode;
    private String preferredPaymentMethodCode;
    private String preferredShippingMethodCode;
    private OffsetDateTime privacyAcceptedAt;
    private String privacyPolicyVersion;
    private Boolean active;
    private Boolean newsletter;

    public String getKeycloakUserId() { return keycloakUserId; }
    public void setKeycloakUserId(String keycloakUserId) { this.keycloakUserId = keycloakUserId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCustomerGroupCode() { return customerGroupCode; }
    public void setCustomerGroupCode(String customerGroupCode) { this.customerGroupCode = customerGroupCode; }
    public String getPreferredPaymentMethodCode() { return preferredPaymentMethodCode; }
    public void setPreferredPaymentMethodCode(String preferredPaymentMethodCode) { this.preferredPaymentMethodCode = preferredPaymentMethodCode; }
    public String getPreferredShippingMethodCode() { return preferredShippingMethodCode; }
    public void setPreferredShippingMethodCode(String preferredShippingMethodCode) { this.preferredShippingMethodCode = preferredShippingMethodCode; }
    public OffsetDateTime getPrivacyAcceptedAt() { return privacyAcceptedAt; }
    public void setPrivacyAcceptedAt(OffsetDateTime privacyAcceptedAt) { this.privacyAcceptedAt = privacyAcceptedAt; }
    public String getPrivacyPolicyVersion() { return privacyPolicyVersion; }
    public void setPrivacyPolicyVersion(String privacyPolicyVersion) { this.privacyPolicyVersion = privacyPolicyVersion; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getNewsletter() { return newsletter; }
    public void setNewsletter(Boolean newsletter) { this.newsletter = newsletter; }
}
