package com.newproject.web.dto;

import java.time.OffsetDateTime;

public class BlogComment {
    private Long id;
    private Long postId;
    private String authorName;
    private String authorEmail;
    private String comment;
    private Boolean approved;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
