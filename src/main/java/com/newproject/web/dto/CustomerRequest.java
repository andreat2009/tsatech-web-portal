package com.newproject.web.dto;

public class CustomerRequest {
    private String keycloakUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
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
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getNewsletter() { return newsletter; }
    public void setNewsletter(Boolean newsletter) { this.newsletter = newsletter; }
}
