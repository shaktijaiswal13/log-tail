package org.taillogs.taillogs.managers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Text;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.taillogs.taillogs.config.PreferencesManager;
import org.taillogs.taillogs.models.HighlightPattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HighlightManager {
    private final ObservableList<HighlightPattern> patterns;

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
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();

        List<Pattern> compiledPatterns = new ArrayList<>();
        List<String> patternColors = new ArrayList<>();

        // Compile enabled patterns
        for (HighlightPattern hp : patterns) {
            if (hp.isEnabled()) {
                try {
                    if (hp.isRegex()) {
                        compiledPatterns.add(Pattern.compile(hp.getPattern()));
                    } else {
                        compiledPatterns.add(Pattern.compile(Pattern.quote(hp.getPattern())));
                    }
                    patternColors.add(hp.getColor());
                } catch (Exception e) {
                    System.err.println("Invalid pattern: " + hp.getPattern());
                }
            }
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

        // Build result with custom highlights overriding base styles
        int lastEnd = 0;
        for (Match match : mergedMatches) {
            // Add base span before match
            if (match.start > lastEnd) {
                int spanStart = lastEnd;
                int spanEnd = match.start;
                int spanLength = spanEnd - spanStart;

                for (int i = spanStart; i < spanEnd; i++) {
                    int pos = i - lastEnd;
                    if (pos == 0) {
                        Collection<String> baseStyle = getBaseStyleAt(baseSpans, spanStart);
                        builder.add(baseStyle, 1);
                    } else {
                        Collection<String> baseStyle = getBaseStyleAt(baseSpans, i);
                        builder.add(baseStyle, 1);
                    }
                }
            }

            // Add custom highlight
            builder.add(Collections.singleton("custom-" + match.color.substring(1)), match.end - match.start);
            lastEnd = match.end;
        }

        // Add remaining base span
        if (lastEnd < text.length()) {
            for (int i = lastEnd; i < text.length(); i++) {
                Collection<String> baseStyle = getBaseStyleAt(baseSpans, i);
                builder.add(baseStyle, 1);
            }
        }

        return builder.create();
    }

    /**
     * Get style at specific position in base spans
     */
    private Collection<String> getBaseStyleAt(StyleSpans<Collection<String>> spans, int position) {
        // StyleSpans provides style at position through iteration
        // For now, return empty collection as fallback
        return Collections.emptyList();
    }

    public void applyCombinedHighlighting(CodeArea codeArea) {
        String text = codeArea.getText();
        if (text.isEmpty()) {
            return;
        }

        // Ensure custom style classes are injected into CodeArea's stylesheet
        ensureStylesLoaded(codeArea);

        StyleSpans<Collection<String>> combined = buildCombinedHighlighting(text);
        codeArea.setStyleSpans(0, combined);
    }

    /**
     * Ensure custom highlight colors are available in CodeArea's stylesheet
     */
    private void ensureStylesLoaded(CodeArea codeArea) {
        // Generate CSS for all enabled custom patterns
        StringBuilder customCss = new StringBuilder();
        for (HighlightPattern pattern : patterns) {
            if (pattern.isEnabled()) {
                String className = "custom-" + pattern.getColor().substring(1);
                customCss.append(".").append(className).append(" { -fx-fill: ").append(pattern.getColor()).append("; }\n");
            }
        }

        if (customCss.length() > 0) {
            String stylesheet = customCss.toString();
            if (!codeArea.getStylesheets().contains(stylesheet)) {
                codeArea.setStyle(codeArea.getStyle() + "\n" + stylesheet);
            }
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
