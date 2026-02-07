package org.taillogs.taillogs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.taillogs.taillogs.models.Bookmark;
import org.taillogs.taillogs.models.FilterRule;
import org.taillogs.taillogs.models.HighlightPattern;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreferencesManager {
    private static final String PREFS_DIR = System.getProperty("user.home") + File.separator + ".tail_logs";
    private static final String PREFS_FILE = PREFS_DIR + File.separator + "preferences.txt";
    private static final String HIGHLIGHTS_FILE = PREFS_DIR + File.separator + "highlights.json";
    private static final String FILTERS_FILE = PREFS_DIR + File.separator + "filters.json";
    private static final String BOOKMARKS_FILE = PREFS_DIR + File.separator + "bookmarks.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static {
        // Create preferences directory if it doesn't exist
        File prefsDir = new File(PREFS_DIR);
        if (!prefsDir.exists()) {
            prefsDir.mkdirs();
        }
    }

    public static AppearanceSettings loadAppearanceSettings() {
        try {
            File file = new File(PREFS_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(PREFS_FILE)));
                String[] lines = content.split("\n");
                if (lines.length >= 2) {
                    int fontSize = Integer.parseInt(lines[0].trim());
                    String fontWeight = lines[1].trim();
                    String backgroundColor = lines.length >= 3 ? lines[2].trim() : "#ffffff";
                    return new AppearanceSettings(fontSize, fontWeight, backgroundColor);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Failed to load preferences: " + e.getMessage());
        }
        return new AppearanceSettings();
    }

    public static void saveAppearanceSettings(AppearanceSettings settings) {
        try {
            String content = settings.getFontSize() + "\n" + settings.getFontWeight() + "\n" + settings.getCodeAreaBackgroundColor();
            Files.write(Paths.get(PREFS_FILE), content.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save preferences: " + e.getMessage());
        }
    }

    // Highlight Pattern methods
    public static void saveHighlightPatterns(List<HighlightPattern> patterns) {
        try {
            String json = gson.toJson(patterns);
            Files.write(Paths.get(HIGHLIGHTS_FILE), json.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save highlight patterns: " + e.getMessage());
        }
    }

    public static List<HighlightPattern> loadHighlightPatterns() {
        try {
            File file = new File(HIGHLIGHTS_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(HIGHLIGHTS_FILE)));
                Type listType = new TypeToken<List<HighlightPattern>>(){}.getType();
                List<HighlightPattern> patterns = gson.fromJson(content, listType);
                return patterns != null ? patterns : new ArrayList<>();
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("Failed to load highlight patterns: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // Filter Rule methods
    public static void saveFilterRules(List<FilterRule> rules) {
        try {
            String json = gson.toJson(rules);
            Files.write(Paths.get(FILTERS_FILE), json.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save filter rules: " + e.getMessage());
        }
    }

    public static List<FilterRule> loadFilterRules() {
        try {
            File file = new File(FILTERS_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(FILTERS_FILE)));
                Type listType = new TypeToken<List<FilterRule>>(){}.getType();
                List<FilterRule> rules = gson.fromJson(content, listType);
                return rules != null ? rules : new ArrayList<>();
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("Failed to load filter rules: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // Bookmark methods
    public static void saveBookmarks(String filePath, List<Bookmark> bookmarks) {
        try {
            String fileKey = encodeFileKey(filePath);
            String bookmarksDataFile = PREFS_DIR + File.separator + "bookmarks_" + fileKey + ".json";
            String json = gson.toJson(bookmarks);
            Files.write(Paths.get(bookmarksDataFile), json.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save bookmarks: " + e.getMessage());
        }
    }

    public static List<Bookmark> loadBookmarks(String filePath) {
        try {
            String fileKey = encodeFileKey(filePath);
            String bookmarksDataFile = PREFS_DIR + File.separator + "bookmarks_" + fileKey + ".json";
            File file = new File(bookmarksDataFile);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(bookmarksDataFile)));
                Type listType = new TypeToken<List<Bookmark>>(){}.getType();
                List<Bookmark> bookmarks = gson.fromJson(content, listType);
                return bookmarks != null ? bookmarks : new ArrayList<>();
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("Failed to load bookmarks: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Encode file path to safe filename
     */
    private static String encodeFileKey(String filePath) {
        if (filePath == null) {
            return "default";
        }
        // Use hash of path to avoid special characters
        return Integer.toHexString(filePath.hashCode());
    }
}
