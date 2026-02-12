package org.taillogs.taillogs.models;

import java.util.ArrayList;
import java.util.List;

public class SavedSettingsProfile {
    private String name;
    private long createdAt;
    private List<HighlightPattern> highlights;
    private List<FilterRule> filters;

    public SavedSettingsProfile() {
        this.highlights = new ArrayList<>();
        this.filters = new ArrayList<>();
    }

    public SavedSettingsProfile(String name, long createdAt, List<HighlightPattern> highlights, List<FilterRule> filters) {
        this.name = name;
        this.createdAt = createdAt;
        this.highlights = highlights != null ? highlights : new ArrayList<>();
        this.filters = filters != null ? filters : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<HighlightPattern> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<HighlightPattern> highlights) {
        this.highlights = highlights;
    }

    public List<FilterRule> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterRule> filters) {
        this.filters = filters;
    }

    public void ensureDefaults() {
        if (highlights == null) {
            highlights = new ArrayList<>();
        }
        if (filters == null) {
            filters = new ArrayList<>();
        }
    }
}
