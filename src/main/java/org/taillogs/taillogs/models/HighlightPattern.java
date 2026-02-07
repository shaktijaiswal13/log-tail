package org.taillogs.taillogs.models;

import java.util.UUID;

public class HighlightPattern {
    private String id;
    private String pattern;
    private String color;
    private boolean isRegex;
    private boolean enabled;

    public HighlightPattern() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.isRegex = false;
        this.color = "#000000";
    }

    public HighlightPattern(String pattern, String color) {
        this();
        this.pattern = pattern;
        this.color = color;
    }

    public HighlightPattern(String pattern, String color, boolean isRegex) {
        this(pattern, color);
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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
        return "HighlightPattern{" +
                "id='" + id + '\'' +
                ", pattern='" + pattern + '\'' +
                ", color='" + color + '\'' +
                ", isRegex=" + isRegex +
                ", enabled=" + enabled +
                '}';
    }
}
