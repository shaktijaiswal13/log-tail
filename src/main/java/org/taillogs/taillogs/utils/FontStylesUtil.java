package org.taillogs.taillogs.utils;

import org.taillogs.taillogs.config.AppearanceSettings;

public class FontStylesUtil {

    public static String getLogTextAreaStyle(AppearanceSettings settings) {
        String fontWeightStyle = settings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        return "-fx-font-family: 'Courier New'; -fx-font-size: " + settings.getFontSize() + "; " +
                fontWeightStyle +
                "-fx-padding: 5; -fx-text-fill: #333333;";
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
        String bgColor = isPaused ? "white" : "#e0e0e0";
        return "-fx-padding: 3 8 3 8; -fx-font-size: " + (settings.getFontSize() - 4) + "; " +
                "-fx-background-color: " + bgColor + "; -fx-text-fill: black; " +
                "-fx-border-color: #333333; -fx-border-width: 1; -fx-border-radius: 0; " +
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
