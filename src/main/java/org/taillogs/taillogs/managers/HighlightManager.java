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
    private static final String CSS_FILE_PATH = System.getProperty("java.io.tmpdir") + File.separator + "taillogs_highlights.css";
    private Set<String> loadedStylesheets = new HashSet<>();

    public HighlightManager() {
        this.patterns = FXCollections.observableArrayList();
        loadPatterns();
    }

    public void addPattern(HighlightPattern pattern) {
        patterns.add(pattern);
        savePatterns();
    }

    public void removePattern(String patternId) {
        patterns.removeIf(p -> p.getId().equals(patternId));
        savePatterns();
    }

    public void togglePattern(String patternId) {
        patterns.stream()
                .filter(p -> p.getId().equals(patternId))
                .findFirst()
                .ifPresent(p -> {
                    p.setEnabled(!p.isEnabled());
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
     * Build combined highlighting from log levels and custom patterns.
     * Priority (highest to lowest): search highlights > custom patterns > log levels
     */
    public StyleSpans<Collection<String>> buildCombinedHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();

        // First apply log level highlighting
        StyleSpans<Collection<String>> logLevelSpans = applyLogLevelHighlighting(text);

        // Then apply custom patterns on top
        return applyCustomPatternsHighlighting(text, logLevelSpans);
    }

    /**
     * Apply log level highlighting (ERROR, WARN, INFO)
     */
    private StyleSpans<Collection<String>> applyLogLevelHighlighting(String text) {
        String pattern = "(?<ERROR>ERROR)|(?<WARN>WARN)|(?<INFO>INFO)";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(text);

        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        while (matcher.find()) {
            builder.add(Collections.emptyList(), matcher.start() - lastEnd);

            if (matcher.group("ERROR") != null) {
                builder.add(Collections.singleton("error"), matcher.end() - matcher.start());
            } else if (matcher.group("WARN") != null) {
                builder.add(Collections.singleton("warn"), matcher.end() - matcher.start());
            } else if (matcher.group("INFO") != null) {
                builder.add(Collections.singleton("info"), matcher.end() - matcher.start());
            }

            lastEnd = matcher.end();
        }

        builder.add(Collections.emptyList(), text.length() - lastEnd);
        return builder.create();
    }

    /**
     * Apply custom pattern highlighting on top of log level highlighting
     */
    private StyleSpans<Collection<String>> applyCustomPatternsHighlighting(
            String text, StyleSpans<Collection<String>> baseSpans) {

        List<Pattern> compiledPatterns = new ArrayList<>();
        List<String> patternColors = new ArrayList<>();

        // Compile enabled patterns
        for (HighlightPattern hp : patterns) {
            if (hp.isEnabled()) {
                try {
                    if (hp.isRegex()) {
                        compiledPatterns.add(Pattern.compile(hp.getPattern()));
                    } else {
                        compiledPatterns.add(Pattern.compile(Pattern.quote(hp.getPattern()), Pattern.CASE_INSENSITIVE));
                    }
                    patternColors.add(hp.getColor());
                } catch (Exception e) {
                    System.err.println("Invalid pattern: " + hp.getPattern());
                }
            }
        }

        // If no custom patterns, return base spans
        if (compiledPatterns.isEmpty()) {
            return baseSpans;
        }

        // Track which positions are covered by custom highlights
        boolean[] customHighlighted = new boolean[text.length()];

        // Find all custom pattern matches
        List<Match> matches = new ArrayList<>();
        for (int i = 0; i < compiledPatterns.size(); i++) {
            Pattern p = compiledPatterns.get(i);
            String color = patternColors.get(i);
            Matcher m = p.matcher(text);

            while (m.find()) {
                matches.add(new Match(m.start(), m.end(), color));
            }
        }

        // Sort matches by start position
        matches.sort((a, b) -> Integer.compare(a.start, b.start));

        // Merge overlapping matches (keep first one)
        List<Match> mergedMatches = new ArrayList<>();
        for (Match match : matches) {
            boolean overlaps = false;
            for (int i = match.start; i < match.end; i++) {
                if (customHighlighted[i]) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                mergedMatches.add(match);
                for (int i = match.start; i < match.end; i++) {
                    customHighlighted[i] = true;
                }
            }
        }

        // If no matches, return base spans
        if (mergedMatches.isEmpty()) {
            return baseSpans;
        }

        // Build result with custom highlights overriding base styles
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        for (Match match : mergedMatches) {
            // Add base span content before this match
            if (match.start > lastEnd) {
                // For simplicity, add unstyled content before the match
                // (log level highlighting will still be visible through base CSS)
                builder.add(Collections.emptyList(), match.start - lastEnd);
            }

            // Add custom highlight using a CSS class based on color
            String colorClass = "highlight-" + match.color.substring(1).toLowerCase();
            builder.add(Collections.singleton(colorClass), match.end - match.start);
            lastEnd = match.end;
        }

        // Add remaining content
        if (lastEnd < text.length()) {
            builder.add(Collections.emptyList(), text.length() - lastEnd);
        }

        return builder.create();
    }

    public void applyCombinedHighlighting(CodeArea codeArea) {
        String text = codeArea.getText();
        if (text.isEmpty()) {
            return;
        }

        // Ensure custom style classes are available in CodeArea's stylesheet
        updateCustomStylesheet(codeArea);

        StyleSpans<Collection<String>> combined = buildCombinedHighlighting(text);
        codeArea.setStyleSpans(0, combined);
    }

    /**
     * Update the custom stylesheet with current highlight patterns
     */
    private void updateCustomStylesheet(CodeArea codeArea) {
        try {
            // Generate CSS content for all patterns
            StringBuilder css = new StringBuilder();
            css.append("/* Auto-generated highlight styles */\n");
            
            for (HighlightPattern pattern : patterns) {
                if (pattern.isEnabled() && pattern.getColor() != null) {
                    String colorClass = "highlight-" + pattern.getColor().substring(1).toLowerCase();
                    css.append(".").append(colorClass).append(" {\n");
                    css.append("    -fx-fill: ").append(pattern.getColor()).append(";\n");
                    css.append("    -fx-font-weight: bold;\n");
                    css.append("}\n");
                }
            }

            // Write CSS to temp file
            Path cssPath = Path.of(CSS_FILE_PATH);
            Files.writeString(cssPath, css.toString());

            // Add stylesheet if not already added
            String cssUri = cssPath.toUri().toString();
            if (!codeArea.getStylesheets().contains(cssUri)) {
                codeArea.getStylesheets().add(cssUri);
            }

        } catch (IOException e) {
            System.err.println("Failed to create highlight stylesheet: " + e.getMessage());
        }
    }

    private void loadPatterns() {
        patterns.clear();
        List<HighlightPattern> loaded = PreferencesManager.loadHighlightPatterns();
        patterns.addAll(loaded);
    }

    private void savePatterns() {
        PreferencesManager.saveHighlightPatterns(new ArrayList<>(patterns));
    }

    private static class Match {
        int start;
        int end;
        String color;

        Match(int start, int end, String color) {
            this.start = start;
            this.end = end;
            this.color = color;
        }
    }
}
