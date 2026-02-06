package org.taillogs.taillogs.config;

public class AppearanceSettings {
    private int fontSize = 13;
    private String fontWeight = "Regular";

    public AppearanceSettings() {
    }

    public AppearanceSettings(int fontSize, String fontWeight) {
        this.fontSize = fontSize;
        this.fontWeight = fontWeight;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontWeight() {
        return fontWeight;
    }

    public void setFontWeight(String fontWeight) {
        this.fontWeight = fontWeight;
    }

    public String getFontWeightStyle() {
        return fontWeight.equals("Bold") ? "-fx-font-weight: bold;" : "";
    }

    @Override
    public String toString() {
        return "AppearanceSettings{" +
                "fontSize=" + fontSize +
                ", fontWeight='" + fontWeight + '\'' +
                '}';
    }
}
