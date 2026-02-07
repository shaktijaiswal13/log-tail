package org.taillogs.taillogs.utils;

import org.taillogs.taillogs.config.AppearanceSettings;

public class FontStylesUtil {

    public static String getLogTextAreaStyle(AppearanceSettings settings) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        String backgroundColor = settings.getCodeAreaBackgroundColor();
        return "-fx-font-family: 'Courier New'; -fx-font-size: " + settings.getFontSize() + "; " +
                fontWeightStyle +
                "-fx-padding: 5; -fx-text-fill: #000000; " +
                "-fx-background-color: " + backgroundColor + ";";
    }

    public static String getMenuBarStyle(AppearanceSettings settings) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        return "-fx-background-color: #2196F3; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: " + (settings.getFontSize() - 2) + "; " +
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
                "-fx-text-fill: #666666; " +
                fontWeightStyle;
    }

    public static String getSearchFieldStyle(AppearanceSettings settings) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        return "-fx-font-size: " + (settings.getFontSize() - 2) + "; " +
                "-fx-padding: 5; -fx-border-color: #cccccc; -fx-border-width: 1; " +
                fontWeightStyle;
    }

    public static String getLabelStyle(AppearanceSettings settings, int fontSizeOffset) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        return "-fx-font-size: " + (settings.getFontSize() + fontSizeOffset) + "; " +
                fontWeightStyle;
    }
}
