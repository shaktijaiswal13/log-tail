package org.taillogs.taillogs.utils;

import org.taillogs.taillogs.config.AppearanceSettings;

public class FontStylesUtil {

    public static String getLogTextAreaStyle(AppearanceSettings settings) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        String backgroundColor = settings.getCodeAreaBackgroundColor();
        return "-fx-font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Courier New', monospace; " +
                "-fx-font-size: " + settings.getFontSize() + "; " +
                fontWeightStyle +
                "-fx-padding: 8 10 8 10; -fx-text-fill: #1F2933; " +
                "-fx-background-color: " + backgroundColor + ";";
    }

    public static String getMenuBarStyle(AppearanceSettings settings) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        return "-fx-background-color: linear-gradient(to right, #264653, #2A9D8F); " +
                "-fx-text-fill: white; " +
                "-fx-font-size: " + (settings.getFontSize() - 1) + "; " +
                fontWeightStyle +
                "-fx-padding: 5px;";
    }

    public static String getButtonStyle(AppearanceSettings settings, boolean isPaused) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        String bgColor = isPaused ? "#1976D2" : "#2196F3";
        String textColor = "white";
        return "-fx-padding: 6 14 6 14; -fx-font-size: " + (settings.getFontSize() - 3) + "; " +
                "-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; " +
                "-fx-border-width: 0; -fx-border-radius: 3; -fx-cursor: hand; " +
                fontWeightStyle;
    }

    public static String getStatusLabelStyle(AppearanceSettings settings) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        return "-fx-font-size: " + (settings.getFontSize() - 2) + "; " +
                "-fx-text-fill: #5A6B7A; " +
                fontWeightStyle;
    }

    public static String getSearchFieldStyle(AppearanceSettings settings) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        return "-fx-font-size: " + (settings.getFontSize() - 1) + "; " +
                "-fx-padding: 7 10 7 10; " +
                "-fx-background-color: #F3F6F9; " +
                "-fx-border-color: #D8DEE5; " +
                "-fx-border-radius: 8px; -fx-background-radius: 8px; " +
                fontWeightStyle;
    }

    public static String getLabelStyle(AppearanceSettings settings, int fontSizeOffset) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        return "-fx-font-size: " + (settings.getFontSize() + fontSizeOffset) + "; " +
                fontWeightStyle;
    }
}
