package com.newproject.web.dto;

import java.time.OffsetDateTime;

public class BlogPostRequest {
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String author;
    private OffsetDateTime publishedAt;
    private Boolean active;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
