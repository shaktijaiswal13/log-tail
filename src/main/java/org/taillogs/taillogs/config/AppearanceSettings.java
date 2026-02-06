package org.taillogs.taillogs.config;

public class AppearanceSettings {
    private int fontSize = 13;
    private String fontWeight = "Regular";
    private String codeAreaBackgroundColor = "#ffffff";

    public AppearanceSettings() {
    }

    public AppearanceSettings(int fontSize, String fontWeight, String codeAreaBackgroundColor) {
        this.fontSize = fontSize;
        this.fontWeight = fontWeight;
        this.codeAreaBackgroundColor = codeAreaBackgroundColor;
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

    public String getCodeAreaBackgroundColor() {
        return codeAreaBackgroundColor;
    }

    public void setCodeAreaBackgroundColor(String codeAreaBackgroundColor) {
        this.codeAreaBackgroundColor = codeAreaBackgroundColor;
    }

    @Override
    public String toString() {
        return "AppearanceSettings{" +
                "fontSize=" + fontSize +
                ", fontWeight='" + fontWeight + '\'' +
                ", codeAreaBackgroundColor='" + codeAreaBackgroundColor + '\'' +
                '}';
    }
}
