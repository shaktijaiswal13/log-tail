package org.taillogs.taillogs.config;

import java.util.HashMap;
import java.util.Map;

public class AppConfig {
    private static final Map<String, Map<String, String>> THEMES = new HashMap<>();

    static {
        // Light Theme
        Map<String, String> lightTheme = new HashMap<>();
        lightTheme.put("bg", "#ffffff");
        lightTheme.put("fg", "#333333");
        lightTheme.put("menubar_bg", "#333333");
        lightTheme.put("menubar_fg", "white");
        lightTheme.put("toolbar_bg", "#ffffff");
        lightTheme.put("sidebar_bg", "#ffffff");
        lightTheme.put("text_bg", "#ffffff");
        lightTheme.put("text_fg", "#333333");
        THEMES.put("light", lightTheme);

        // Dark Theme
        Map<String, String> darkTheme = new HashMap<>();
        darkTheme.put("bg", "#1e1e1e");
        darkTheme.put("fg", "#e0e0e0");
        darkTheme.put("menubar_bg", "#1a1a1a");
        darkTheme.put("menubar_fg", "#e0e0e0");
        darkTheme.put("toolbar_bg", "#252525");
        darkTheme.put("sidebar_bg", "#252525");
        darkTheme.put("text_bg", "#0d0d0d");
        darkTheme.put("text_fg", "#00ff00");
        THEMES.put("dark", darkTheme);

        // Monokai Theme
        Map<String, String> monokaiTheme = new HashMap<>();
        monokaiTheme.put("bg", "#272822");
        monokaiTheme.put("fg", "#f8f8f2");
        monokaiTheme.put("menubar_bg", "#1e1e1e");
        monokaiTheme.put("menubar_fg", "#f8f8f2");
        monokaiTheme.put("toolbar_bg", "#3e3d32");
        monokaiTheme.put("sidebar_bg", "#3e3d32");
        monokaiTheme.put("text_bg", "#272822");
        monokaiTheme.put("text_fg", "#a6e22e");
        THEMES.put("monokai", monokaiTheme);
    }

    public static final String DEFAULT_THEME = "light";

    public static Map<String, String> getTheme(String themeName) {
        return THEMES.getOrDefault(themeName, THEMES.get(DEFAULT_THEME));
    }

    public static Map<String, String> getDefaultTheme() {
        return THEMES.get(DEFAULT_THEME);
    }

    public static boolean themeExists(String themeName) {
        return THEMES.containsKey(themeName);
    }

    public static String toHexColor(String color) {
        return color;
    }

    public static javafx.scene.paint.Color parseColor(String hexColor) {
        return javafx.scene.paint.Color.web(hexColor);
    }
}
