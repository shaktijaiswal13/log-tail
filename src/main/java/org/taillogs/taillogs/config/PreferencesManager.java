package org.taillogs.taillogs.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PreferencesManager {
    private static final String PREFS_DIR = System.getProperty("user.home") + File.separator + ".tail_logs";
    private static final String PREFS_FILE = PREFS_DIR + File.separator + "preferences.txt";

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
}
