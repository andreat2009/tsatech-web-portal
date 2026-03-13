package com.newproject.web.dto;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsSummary {
    private long totalVisitors;
    private long totalPageViews;
    private long todayVisitors;
    private long uniqueProductsViewed;
    private List<PathStat> topPaths = new ArrayList<>();

    public long getTotalVisitors() {
        return totalVisitors;
    }

    public void setTotalVisitors(long totalVisitors) {
        this.totalVisitors = totalVisitors;
    }

    public long getTotalPageViews() {
        return totalPageViews;
    }

    public void setTotalPageViews(long totalPageViews) {
        this.totalPageViews = totalPageViews;
    }

    public long getTodayVisitors() {
        return todayVisitors;
    }

    public void setTodayVisitors(long todayVisitors) {
        this.todayVisitors = todayVisitors;
    }

    public long getUniqueProductsViewed() {
        return uniqueProductsViewed;
    }

    public void setUniqueProductsViewed(long uniqueProductsViewed) {
        this.uniqueProductsViewed = uniqueProductsViewed;
    }

    public List<PathStat> getTopPaths() {
        return topPaths;
    }

    public void setTopPaths(List<PathStat> topPaths) {
        this.topPaths = topPaths;
    }

    public static class PathStat {
        private String path;
        private long views;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public long getViews() {
            return views;
        }

        public void setViews(long views) {
            this.views = views;
        }
    }
}
