package com.newproject.web.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class InformationRequest {
    private String title;
    private String slug;
    private String content;
    private String seoKeywords;
    private Integer sortOrder;
    private Boolean active;
    private Map<String, LocalizedContent> translations = new LinkedHashMap<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSeoKeywords() { return seoKeywords; }
    public void setSeoKeywords(String seoKeywords) { this.seoKeywords = seoKeywords; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Map<String, LocalizedContent> getTranslations() { return translations; }
    public void setTranslations(Map<String, LocalizedContent> translations) { this.translations = translations; }
}
