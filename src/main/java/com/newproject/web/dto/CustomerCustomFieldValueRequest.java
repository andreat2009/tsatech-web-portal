package com.newproject.web.dto;

public class CustomerCustomFieldValueRequest {
    private Long customFieldId;
    private String customFieldCode;
    private String value;

    public Long getCustomFieldId() { return customFieldId; }
    public void setCustomFieldId(Long customFieldId) { this.customFieldId = customFieldId; }
    public String getCustomFieldCode() { return customFieldCode; }
    public void setCustomFieldCode(String customFieldCode) { this.customFieldCode = customFieldCode; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
