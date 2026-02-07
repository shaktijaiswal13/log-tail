package org.taillogs.taillogs.managers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.taillogs.taillogs.config.PreferencesManager;
import org.taillogs.taillogs.models.Bookmark;

import java.util.List;

public class BookmarkManager {
    private final ObservableList<Bookmark> bookmarks;
    private String currentFilePath;

    public BookmarkManager() {
        this.bookmarks = FXCollections.observableArrayList();
    }

    public void setCurrentFile(String filePath) {
        this.currentFilePath = filePath;
        loadBookmarks();
    }

    public void addBookmark(int lineNumber, String linePreview) {
        if (currentFilePath == null) return;

        Bookmark bookmark = new Bookmark(lineNumber, linePreview);
        bookmarks.add(bookmark);
        saveBookmarks();
    }

    public void removeBookmark(String bookmarkId) {
        bookmarks.removeIf(b -> b.getId().equals(bookmarkId));
        saveBookmarks();
    }

    public void removeBookmarkByLine(int lineNumber) {
        bookmarks.removeIf(b -> b.getLineNumber() == lineNumber);
        saveBookmarks();
    }

    public void clearBookmarks() {
        bookmarks.clear();
        saveBookmarks();
    }

    public ObservableList<Bookmark> getBookmarks() {
        return bookmarks;
    }

    public boolean isLineBookmarked(int lineNumber) {
        return bookmarks.stream().anyMatch(b -> b.getLineNumber() == lineNumber);
    }

    public Bookmark getBookmarkByLine(int lineNumber) {
        return bookmarks.stream()
                .filter(b -> b.getLineNumber() == lineNumber)
                .findFirst()
                .orElse(null);
    }

    private void loadBookmarks() {
        bookmarks.clear();
        if (currentFilePath != null) {
            List<Bookmark> loaded = PreferencesManager.loadBookmarks(currentFilePath);
            bookmarks.addAll(loaded);
        }
    }

    private void saveBookmarks() {
        if (currentFilePath != null) {
            PreferencesManager.saveBookmarks(currentFilePath, new java.util.ArrayList<>(bookmarks));
        }
    }
}
