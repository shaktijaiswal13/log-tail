package org.taillogs.taillogs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.taillogs.taillogs.models.Bookmark;
import org.taillogs.taillogs.models.FilterRule;
import org.taillogs.taillogs.models.HighlightPattern;
import org.taillogs.taillogs.models.RecentFile;

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
    private static final String PROJECT_SETTINGS_FILE = PREFS_DIR + File.separator + "project_settings.json";
    private static final String BOOKMARKS_FILE = PREFS_DIR + File.separator + "bookmarks.json";
    private static final String RECENT_FILES_FILE = PREFS_DIR + File.separator + "recent_files.json";
    private static final int MAX_RECENT_FILES = 10;
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

    // Project settings methods
    public static void saveProjectSettings(ProjectSettings settings) {
        try {
            String json = gson.toJson(settings);
            Files.write(Paths.get(PROJECT_SETTINGS_FILE), json.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save project settings: " + e.getMessage());
        }
    }

    public static ProjectSettings loadProjectSettings() {
        try {
            File file = new File(PROJECT_SETTINGS_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(PROJECT_SETTINGS_FILE)));
                ProjectSettings settings = gson.fromJson(content, ProjectSettings.class);
                if (settings != null) {
                    settings.ensureDefaults();
                    return settings;
                }
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("Failed to load project settings: " + e.getMessage());
        }
        return new ProjectSettings();
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

    // Per-file enabled state methods
    public static void saveHighlightStates(String filePath, Map<String, Boolean> states) {
        try {
            String fileKey = encodeFileKey(filePath);
            String statesFile = PREFS_DIR + File.separator + "highlight_states_" + fileKey + ".json";
            String json = gson.toJson(states);
            Files.write(Paths.get(statesFile), json.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save highlight states: " + e.getMessage());
        }
    }

    public static Map<String, Boolean> loadHighlightStates(String filePath) {
        try {
            String fileKey = encodeFileKey(filePath);
            String statesFile = PREFS_DIR + File.separator + "highlight_states_" + fileKey + ".json";
            File file = new File(statesFile);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(statesFile)));
                Type mapType = new TypeToken<Map<String, Boolean>>() {}.getType();
                Map<String, Boolean> states = gson.fromJson(content, mapType);
                return states != null ? states : new HashMap<>();
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("Failed to load highlight states: " + e.getMessage());
        }
        return new HashMap<>();
    }

    public static void saveFilterStates(String filePath, Map<String, Boolean> states) {
        try {
            String fileKey = encodeFileKey(filePath);
            String statesFile = PREFS_DIR + File.separator + "filter_states_" + fileKey + ".json";
            String json = gson.toJson(states);
            Files.write(Paths.get(statesFile), json.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save filter states: " + e.getMessage());
        }
    }

    public static Map<String, Boolean> loadFilterStates(String filePath) {
        try {
            String fileKey = encodeFileKey(filePath);
            String statesFile = PREFS_DIR + File.separator + "filter_states_" + fileKey + ".json";
            File file = new File(statesFile);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(statesFile)));
                Type mapType = new TypeToken<Map<String, Boolean>>() {}.getType();
                Map<String, Boolean> states = gson.fromJson(content, mapType);
                return states != null ? states : new HashMap<>();
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("Failed to load filter states: " + e.getMessage());
        }
        return new HashMap<>();
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

    // Per-file highlight pattern methods
    public static void saveHighlightPatterns(String filePath, List<HighlightPattern> patterns) {
        try {
            String fileKey = encodeFileKey(filePath);
            String highlightsDataFile = PREFS_DIR + File.separator + "highlights_" + fileKey + ".json";
            String json = gson.toJson(patterns);
            Files.write(Paths.get(highlightsDataFile), json.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save per-file highlight patterns: " + e.getMessage());
        }
    }

    public static List<HighlightPattern> loadHighlightPatterns(String filePath) {
        try {
            String fileKey = encodeFileKey(filePath);
            String highlightsDataFile = PREFS_DIR + File.separator + "highlights_" + fileKey + ".json";
            File file = new File(highlightsDataFile);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(highlightsDataFile)));
                Type listType = new TypeToken<List<HighlightPattern>>(){}.getType();
                List<HighlightPattern> patterns = gson.fromJson(content, listType);
                return patterns != null ? patterns : new ArrayList<>();
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("Failed to load per-file highlight patterns: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // Per-file filter rule methods
    public static void saveFilterRules(String filePath, List<FilterRule> rules) {
        try {
            String fileKey = encodeFileKey(filePath);
            String filtersDataFile = PREFS_DIR + File.separator + "filters_" + fileKey + ".json";
            String json = gson.toJson(rules);
            Files.write(Paths.get(filtersDataFile), json.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save per-file filter rules: " + e.getMessage());
        }
    }

    public static List<FilterRule> loadFilterRules(String filePath) {
        try {
            String fileKey = encodeFileKey(filePath);
            String filtersDataFile = PREFS_DIR + File.separator + "filters_" + fileKey + ".json";
            File file = new File(filtersDataFile);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(filtersDataFile)));
                Type listType = new TypeToken<List<FilterRule>>(){}.getType();
                List<FilterRule> rules = gson.fromJson(content, listType);
                return rules != null ? rules : new ArrayList<>();
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("Failed to load per-file filter rules: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // Recent files methods
    public static void addRecentFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        try {
            List<RecentFile> recent = loadRecentFiles();

            // Remove if already exists (to update timestamp)
            recent.removeIf(rf -> rf.getFilePath().equals(filePath));

            // Add to beginning with current timestamp
            String fileName = new File(filePath).getName();
            recent.add(0, new RecentFile(filePath, fileName, System.currentTimeMillis()));

            // Keep only last 10
            if (recent.size() > MAX_RECENT_FILES) {
                recent.subList(MAX_RECENT_FILES, recent.size()).clear();
            }

            // Save
            String json = gson.toJson(recent);
            Files.write(Paths.get(RECENT_FILES_FILE), json.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to add recent file: " + e.getMessage());
        }
    }

    public static List<RecentFile> loadRecentFiles() {
        try {
            File file = new File(RECENT_FILES_FILE);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(RECENT_FILES_FILE)));
                Type listType = new TypeToken<List<RecentFile>>(){}.getType();
                List<RecentFile> recentFiles = gson.fromJson(content, listType);
                return recentFiles != null ? recentFiles : new ArrayList<>();
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("Failed to load recent files: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static void clearRecentFiles() {
        try {
            Files.write(Paths.get(RECENT_FILES_FILE), "[]".getBytes());
        } catch (IOException e) {
            System.err.println("Failed to clear recent files: " + e.getMessage());
        }
    }

    public static void removeRecentFile(String filePath) {
        try {
            List<RecentFile> recent = loadRecentFiles();
            recent.removeIf(rf -> rf.getFilePath().equals(filePath));
            String json = gson.toJson(recent);
            Files.write(Paths.get(RECENT_FILES_FILE), json.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to remove recent file: " + e.getMessage());
        }
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
