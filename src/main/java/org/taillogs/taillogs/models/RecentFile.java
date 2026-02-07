package org.taillogs.taillogs.models;

public class RecentFile {
    private String filePath;
    private String fileName;
    private long lastAccessTime;

    public RecentFile(String filePath, String fileName, long lastAccessTime) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.lastAccessTime = lastAccessTime;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
}
