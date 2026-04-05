package com.newproject.web.dto;

import java.util.List;

public class PagedResponse<T> {
    private List<T> content = List.of();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    public static <T> PagedResponse<T> empty(int page, int size) {
        PagedResponse<T> response = new PagedResponse<>();
        response.setPage(Math.max(0, page));
        response.setSize(Math.max(1, size));
        response.setTotalElements(0);
        response.setTotalPages(0);
        response.setFirst(true);
        response.setLast(true);
        response.setEmpty(true);
        response.setContent(List.of());
        return response;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content == null ? List.of() : content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }
}
