package com.newproject.web.dto;

public class CustomerCustomFieldValue {
    private Long customFieldId;
    private String code;
    private String label;
    private String fieldType;
    private String fieldScope;
    private Boolean persistForCustomer;
    private String value;

    public Long getCustomFieldId() { return customFieldId; }
    public void setCustomFieldId(Long customFieldId) { this.customFieldId = customFieldId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public String getFieldScope() { return fieldScope; }
    public void setFieldScope(String fieldScope) { this.fieldScope = fieldScope; }
    public Boolean getPersistForCustomer() { return persistForCustomer; }
    public void setPersistForCustomer(Boolean persistForCustomer) { this.persistForCustomer = persistForCustomer; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
