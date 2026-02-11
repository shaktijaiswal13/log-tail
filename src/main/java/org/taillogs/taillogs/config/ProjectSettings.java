package org.taillogs.taillogs.config;

import org.taillogs.taillogs.models.FilterRule;
import org.taillogs.taillogs.models.HighlightPattern;

import java.util.ArrayList;
import java.util.List;

public class ProjectSettings {
    private List<HighlightPattern> highlights;
    private List<FilterRule> filters;

    public ProjectSettings() {
        this.highlights = new ArrayList<>();
        this.filters = new ArrayList<>();
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
