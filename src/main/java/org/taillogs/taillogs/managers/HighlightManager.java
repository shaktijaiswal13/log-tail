package org.taillogs.taillogs.managers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Text;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.taillogs.taillogs.config.PreferencesManager;
import org.taillogs.taillogs.models.HighlightPattern;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HighlightManager {
    private final ObservableList<HighlightPattern> patterns;
    private static final String CSS_FILE_BASE = System.getProperty("java.io.tmpdir") + File.separator + "taillogs_highlights";
    private String currentCssPath = null;
    private long cssVersion = 0;
    private String currentFilePath;

    public HighlightManager() {
        this.patterns = FXCollections.observableArrayList();
        System.out.println("[HighlightManager] Initialized (no file loaded yet)");
    }

    public void setCurrentFile(String filePath) {
        this.currentFilePath = filePath;
        loadPatterns();
        System.out.println("[HighlightManager] Set current file to: " + filePath);
    }

    public void addPattern(HighlightPattern pattern) {
        System.out.println("[HighlightManager] Adding pattern: " + pattern.getPattern() + " with color: " + pattern.getColor());
        patterns.add(pattern);
        savePatterns();
    }

    public void removePattern(String patternId) {
        System.out.println("[HighlightManager] Removing pattern: " + patternId);
        patterns.removeIf(p -> p.getId().equals(patternId));
        savePatterns();
    }

    public void togglePattern(String patternId) {
        patterns.stream()
                .filter(p -> p.getId().equals(patternId))
                .findFirst()
                .ifPresent(p -> {
                    p.setEnabled(!p.isEnabled());
                    System.out.println("[HighlightManager] Toggled pattern " + p.getPattern() + " to enabled=" + p.isEnabled());
                    savePatterns();
                });
    }

    public ObservableList<HighlightPattern> getPatterns() {
        return patterns;
    }

    public void clearPatterns() {
        patterns.clear();
        savePatterns();
    }

    /**
     * Convert a JavaFX color string to a proper CSS hex color.
     * JavaFX ColorPicker returns colors like "0xff0000ff" (RRGGBBAA format).
     * We need to convert this to "#ff0000" (CSS RGB format).
     */
    private String normalizeColor(String color) {
        if (color == null || color.isEmpty()) {
            return "#ff0000"; // Default to red
        }
        
        // If already in proper CSS format, return as-is
        if (color.startsWith("#") && color.length() == 7) {
            return color.toLowerCase();
        }
        
        // Handle "0xRRGGBBAA" format from JavaFX
        if (color.startsWith("0x") && color.length() == 10) {
            // Extract RGB portion (skip "0x" and last 2 chars which are alpha)
            String rgb = color.substring(2, 8);
            return "#" + rgb.toLowerCase();
        }
        
        // Handle "#RRGGBBAA" format
        if (color.startsWith("#") && color.length() == 9) {
            return "#" + color.substring(1, 7).toLowerCase();
        }
        
        // Handle other formats - try to clean up
        String cleaned = color.replace("0x", "#");
        if (cleaned.length() > 7) {
            cleaned = cleaned.substring(0, 7);
        }
        
        System.out.println("[HighlightManager] Normalized color from '" + color + "' to '" + cleaned + "'");
        return cleaned.toLowerCase();
    }

    /**
     * Build combined highlighting from log levels and custom patterns.
     * Priority (highest to lowest): search highlights > custom patterns > log levels
     */
    public StyleSpans<Collection<String>> buildCombinedHighlighting(String text) {
        System.out.println("[HighlightManager] buildCombinedHighlighting called, text length: " + text.length());
        
        // Collect all enabled patterns with their compiled regex
        List<CompiledPattern> compiledPatterns = new ArrayList<>();
        
        // Add log level patterns first (lower priority)
        compiledPatterns.add(new CompiledPattern(Pattern.compile("ERROR", Pattern.CASE_INSENSITIVE), "error", false));
        compiledPatterns.add(new CompiledPattern(Pattern.compile("WARN", Pattern.CASE_INSENSITIVE), "warn", false));
        compiledPatterns.add(new CompiledPattern(Pattern.compile("INFO", Pattern.CASE_INSENSITIVE), "info", false));
        
        // Add custom patterns (higher priority - will override log levels)
        int customPatternCount = 0;
        for (HighlightPattern hp : patterns) {
            if (hp.isEnabled()) {
                try {
                    Pattern regex;
                    if (hp.isRegex()) {
                        regex = Pattern.compile(hp.getPattern(), Pattern.CASE_INSENSITIVE);
                    } else {
                        regex = Pattern.compile(Pattern.quote(hp.getPattern()), Pattern.CASE_INSENSITIVE);
                    }
                    String normalizedColor = normalizeColor(hp.getColor());
                    String colorClass = "highlight-" + normalizedColor.substring(1);
                    compiledPatterns.add(new CompiledPattern(regex, colorClass, true));
                    customPatternCount++;
                    System.out.println("[HighlightManager] Added custom pattern: " + hp.getPattern() + " -> class: " + colorClass);
                } catch (Exception e) {
                    System.err.println("[HighlightManager] Invalid regex pattern: " + hp.getPattern() + " - " + e.getMessage());
                }
            }
        }
        
        System.out.println("[HighlightManager] Total patterns to apply: " + compiledPatterns.size() + " (custom: " + customPatternCount + ")");
        
        // Find all matches for all patterns
        List<Match> allMatches = new ArrayList<>();
        for (CompiledPattern cp : compiledPatterns) {
            Matcher m = cp.pattern.matcher(text);
            while (m.find()) {
                allMatches.add(new Match(m.start(), m.end(), cp.styleClass, cp.isCustom));
            }
        }
        
        System.out.println("[HighlightManager] Found " + allMatches.size() + " total matches");
        
        // Sort matches by start position, then by priority (custom patterns win)
        allMatches.sort((a, b) -> {
            int startCompare = Integer.compare(a.start, b.start);
            if (startCompare != 0) return startCompare;
            // If same start, custom patterns have higher priority
            if (a.isCustom && !b.isCustom) return -1;
            if (!a.isCustom && b.isCustom) return 1;
            return 0;
        });
        
        // Merge overlapping matches - custom patterns take precedence
        List<Match> finalMatches = new ArrayList<>();
        boolean[] covered = new boolean[text.length()];
        
        // First pass: add all custom pattern matches
        for (Match match : allMatches) {
            if (match.isCustom) {
                boolean hasOverlap = false;
                for (int i = match.start; i < match.end && i < text.length(); i++) {
                    if (covered[i]) {
                        hasOverlap = true;
                        break;
                    }
                }
                if (!hasOverlap) {
                    finalMatches.add(match);
                    for (int i = match.start; i < match.end && i < text.length(); i++) {
                        covered[i] = true;
                    }
                }
            }
        }
        
        // Second pass: add log level matches that don't overlap with custom
        for (Match match : allMatches) {
            if (!match.isCustom) {
                boolean hasOverlap = false;
                for (int i = match.start; i < match.end && i < text.length(); i++) {
                    if (covered[i]) {
                        hasOverlap = true;
                        break;
                    }
                }
                if (!hasOverlap) {
                    finalMatches.add(match);
                    for (int i = match.start; i < match.end && i < text.length(); i++) {
                        covered[i] = true;
                    }
                }
            }
        }
        
        // Sort final matches by position
        finalMatches.sort((a, b) -> Integer.compare(a.start, b.start));
        
        System.out.println("[HighlightManager] Final matches after merging: " + finalMatches.size());
        
        // Build style spans
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;
        
        for (Match match : finalMatches) {
            if (match.start > lastEnd) {
                builder.add(Collections.emptyList(), match.start - lastEnd);
            }
            builder.add(Collections.singleton(match.styleClass), match.end - match.start);
            lastEnd = match.end;
        }
        
        // Add remaining unstyled content
        if (lastEnd < text.length()) {
            builder.add(Collections.emptyList(), text.length() - lastEnd);
        }
        
        return builder.create();
    }

    public void applyCombinedHighlighting(CodeArea codeArea) {
        System.out.println("[HighlightManager] applyCombinedHighlighting called");
        
        String text = codeArea.getText();
        if (text.isEmpty()) {
            System.out.println("[HighlightManager] Text is empty, skipping highlighting");
            return;
        }
        
        System.out.println("[HighlightManager] Text length: " + text.length() + ", patterns count: " + patterns.size());

        // Ensure custom style classes are available in CodeArea's stylesheet
        updateCustomStylesheet(codeArea);

        try {
            StyleSpans<Collection<String>> combined = buildCombinedHighlighting(text);
            codeArea.setStyleSpans(0, combined);
            System.out.println("[HighlightManager] Successfully applied style spans");
        } catch (Exception e) {
            System.err.println("[HighlightManager] Error applying style spans: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update the custom stylesheet with current highlight patterns.
     * Uses versioned filenames to force JavaFX to reload the stylesheet.
     */
    private void updateCustomStylesheet(CodeArea codeArea) {
        System.out.println("[HighlightManager] updateCustomStylesheet called");
        
        try {
            // Increment version to force stylesheet reload
            cssVersion++;
            String newCssPath = CSS_FILE_BASE + "_v" + cssVersion + ".css";
            
            // Generate CSS content for all patterns
            StringBuilder css = new StringBuilder();
            css.append("/* Auto-generated highlight styles - version ").append(cssVersion).append(" */\n\n");
            
            // Add base log level styles (in case main stylesheet isn't loaded)
            css.append(".error { -fx-fill: #cc0000; -fx-font-weight: bold; }\n");
            css.append(".warn { -fx-fill: #ff8800; }\n");
            css.append(".info { -fx-fill: #0066cc; }\n\n");
            
            int styleCount = 0;
            for (HighlightPattern pattern : patterns) {
                if (pattern.isEnabled() && pattern.getColor() != null) {
                    String normalizedColor = normalizeColor(pattern.getColor());
                    String colorClass = "highlight-" + normalizedColor.substring(1);
                    css.append(".").append(colorClass).append(" {\n");
                    css.append("    -fx-fill: ").append(normalizedColor).append(";\n");
                    css.append("    -fx-font-weight: bold;\n");
                    css.append("}\n\n");
                    styleCount++;
                    System.out.println("[HighlightManager] Generated CSS for class: ." + colorClass + " with color: " + normalizedColor);
                }
            }
            
            System.out.println("[HighlightManager] Generated " + styleCount + " custom styles");

            // Write CSS to new temp file
            Path newPath = Path.of(newCssPath);
            Files.writeString(newPath, css.toString());
            System.out.println("[HighlightManager] Wrote CSS to: " + newCssPath);
            
            // Debug: print the CSS content
            System.out.println("[HighlightManager] CSS content:\n" + css.toString());

            // Remove old stylesheet if exists
            if (currentCssPath != null) {
                String oldUri = Path.of(currentCssPath).toUri().toString();
                codeArea.getStylesheets().remove(oldUri);
                System.out.println("[HighlightManager] Removed old stylesheet: " + oldUri);
                
                // Delete old file
                try {
                    Files.deleteIfExists(Path.of(currentCssPath));
                } catch (Exception e) {
                    // Ignore deletion errors
                }
            }

            // Add new stylesheet
            String newUri = newPath.toUri().toString();
            if (!codeArea.getStylesheets().contains(newUri)) {
                codeArea.getStylesheets().add(newUri);
                System.out.println("[HighlightManager] Added new stylesheet: " + newUri);
            }
            
            currentCssPath = newCssPath;
            
            // Log current stylesheets
            System.out.println("[HighlightManager] Current stylesheets on CodeArea:");
            for (String sheet : codeArea.getStylesheets()) {
                System.out.println("  - " + sheet);
            }

        } catch (IOException e) {
            System.err.println("[HighlightManager] Failed to create highlight stylesheet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadPatterns() {
        patterns.clear();
        List<HighlightPattern> loaded;

        if (currentFilePath != null) {
            // Load per-file patterns
            loaded = PreferencesManager.loadHighlightPatterns(currentFilePath);
            if (loaded.isEmpty()) {
                // Fall back to global patterns if per-file doesn't exist
                List<HighlightPattern> globalPatterns = PreferencesManager.loadHighlightPatterns();
                if (!globalPatterns.isEmpty()) {
                    System.out.println("[HighlightManager] No per-file patterns found, using global patterns as defaults");
                    loaded = globalPatterns;
                }
            }
        } else {
            // Load global patterns if no current file
            loaded = PreferencesManager.loadHighlightPatterns();
        }

        patterns.addAll(loaded);
        System.out.println("[HighlightManager] Loaded " + loaded.size() + " patterns for file: " + currentFilePath);
    }

    private void savePatterns() {
        List<HighlightPattern> patternsCopy = new ArrayList<>(patterns);

        if (currentFilePath != null) {
            // Save per-file patterns
            PreferencesManager.saveHighlightPatterns(currentFilePath, patternsCopy);
        } else {
            // Save global patterns if no current file
            PreferencesManager.saveHighlightPatterns(patternsCopy);
        }
        System.out.println("[HighlightManager] Saved " + patterns.size() + " patterns for file: " + currentFilePath);
    }

    private static class Match {
        int start;
        int end;
        String styleClass;
        boolean isCustom;

        Match(int start, int end, String styleClass, boolean isCustom) {
            this.start = start;
            this.end = end;
            this.styleClass = styleClass;
            this.isCustom = isCustom;
        }
    }
    
    private static class CompiledPattern {
        Pattern pattern;
        String styleClass;
        boolean isCustom;
        
        CompiledPattern(Pattern pattern, String styleClass, boolean isCustom) {
            this.pattern = pattern;
            this.styleClass = styleClass;
            this.isCustom = isCustom;
        }
    }
}
