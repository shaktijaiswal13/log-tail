package org.taillogs.taillogs.models;

import java.util.UUID;

public class FilterRule {
    private String id;
    private String pattern;
    private boolean isRegex;
    private boolean enabled;

    public FilterRule() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.isRegex = false;
    }

    public FilterRule(String pattern) {
        this();
        this.pattern = pattern;
    }

    public FilterRule(String pattern, boolean isRegex) {
        this(pattern);
        this.isRegex = isRegex;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isRegex() {
        return isRegex;
    }

    public void setRegex(boolean regex) {
        isRegex = regex;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "FilterRule{" +
                "id='" + id + '\'' +
                ", pattern='" + pattern + '\'' +
                ", isRegex=" + isRegex +
                ", enabled=" + enabled +
                '}';
    }
}
