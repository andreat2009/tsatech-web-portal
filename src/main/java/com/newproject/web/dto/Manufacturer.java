package com.newproject.web.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class Manufacturer {
    private Long id;
    private String name;
    private Map<String, LocalizedContent> translations = new LinkedHashMap<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, LocalizedContent> getTranslations() { return translations; }
    public void setTranslations(Map<String, LocalizedContent> translations) { this.translations = translations; }
}
