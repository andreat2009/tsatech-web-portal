package com.newproject.web.dto;

public class BlogCommentRequest {
    private String authorName;
    private String authorEmail;
    private String comment;

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
