package com.newproject.web.dto;

import java.time.OffsetDateTime;

public class CustomerDownload {
    private Long id;
    private Long customerId;
    private Long orderId;
    private String name;
    private String filename;
    private Long sizeBytes;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
