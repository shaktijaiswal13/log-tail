package org.taillogs.taillogs.models;

import java.util.UUID;

public class Bookmark {
    private String id;
    private int lineNumber;
    private String linePreview;
    private long timestamp;

    public Bookmark() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public Bookmark(int lineNumber, String linePreview) {
        this();
        this.lineNumber = lineNumber;
        this.linePreview = linePreview;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getLinePreview() {
        return linePreview;
    }

    public void setLinePreview(String linePreview) {
        this.linePreview = linePreview;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Bookmark{" +
                "id='" + id + '\'' +
                ", lineNumber=" + lineNumber +
                ", linePreview='" + linePreview + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
