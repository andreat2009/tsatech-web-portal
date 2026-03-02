package com.newproject.web.dto;

import java.util.ArrayList;
import java.util.List;

public class CategoryTree {
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private List<CategoryTree> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<CategoryTree> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryTree> children) {
        this.children = children;
    }
}
