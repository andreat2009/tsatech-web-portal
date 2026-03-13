package com.newproject.web.dto;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Product {
    private Long id;
    private String sku;
    private String model;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private Boolean active;
    private String image;
    private String seoKeywords;
    private String coverImageUrl;
    private List<ProductImage> galleryImages;
    private Long manufacturerId;
    private Set<Long> categoryIds;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Map<String, LocalizedContent> translations = new LinkedHashMap<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getSeoKeywords() { return seoKeywords; }
    public void setSeoKeywords(String seoKeywords) { this.seoKeywords = seoKeywords; }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public List<ProductImage> getGalleryImages() {
        return galleryImages;
    }

    public void setGalleryImages(List<ProductImage> galleryImages) {
        this.galleryImages = galleryImages;
    }

    public Long getManufacturerId() { return manufacturerId; }
    public void setManufacturerId(Long manufacturerId) { this.manufacturerId = manufacturerId; }
    public Set<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(Set<Long> categoryIds) { this.categoryIds = categoryIds; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, LocalizedContent> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, LocalizedContent> translations) {
        this.translations = translations;
    }

    public String getPrimaryImageUrl() {
        if (coverImageUrl != null && !coverImageUrl.isBlank()) {
            return coverImageUrl;
        }
        if (image != null && !image.isBlank()) {
            return image;
        }
        return null;
    }

    public String getSeoSlug() {
        String source = (name != null && !name.isBlank()) ? name : (sku != null ? sku : "prodotto");
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "prodotto";
        }
        return normalized;
    }

    public String getSeoPath() {
        if (id == null) {
            return "/catalogo";
        }
        return "/catalogo/prodotto/" + id + "-" + getSeoSlug();
    }

    public String getSeoReviewsPath() {
        if (id == null) {
            return "/catalogo";
        }
        return "/catalogo/prodotto/" + id + "-" + getSeoSlug() + "/recensioni";
    }
}
