package org.taillogs.taillogs.managers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.taillogs.taillogs.config.PreferencesManager;
import org.taillogs.taillogs.models.FilterRule;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FilterManager {
    private final ObservableList<FilterRule> rules;
    private String currentFilePath;

    public FilterManager() {
        this.rules = FXCollections.observableArrayList();
    }

    public void setCurrentFile(String filePath) {
        this.currentFilePath = filePath;
        loadRules();
    }

    public void addRule(FilterRule rule) {
        rules.add(rule);
        saveRules();
    }

    public void removeRule(String ruleId) {
        rules.removeIf(r -> r.getId().equals(ruleId));
        saveRules();
    }

    public void toggleRule(String ruleId) {
        rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .ifPresent(r -> {
                    r.setEnabled(!r.isEnabled());
                    saveRules();
                });
    }

    public ObservableList<FilterRule> getRules() {
        return rules;
    }

    public void clearRules() {
        rules.clear();
        saveRules();
    }

    /**
     * Check if a line matches all enabled filter rules (AND logic)
     */
    public boolean matchesFilters(String line) {
        List<FilterRule> enabledRules = rules.stream()
                .filter(FilterRule::isEnabled)
                .collect(Collectors.toList());

        if (enabledRules.isEmpty()) {
            return true; // No filters, show all
        }

        // Line must match all enabled rules
        for (FilterRule rule : enabledRules) {
            if (!matchesRule(line, rule)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if a line matches a specific rule
     */
    private boolean matchesRule(String line, FilterRule rule) {
        try {
            if (rule.isRegex()) {
                Pattern pattern = Pattern.compile(rule.getPattern());
                return pattern.matcher(line).find();
            } else {
                return line.contains(rule.getPattern());
            }
        } catch (Exception e) {
            System.err.println("Invalid filter pattern: " + rule.getPattern());
            return false;
        }
    }

    /**
     * Filter content lines based on enabled rules
     * Returns list of {lineNumber, lineContent} for matching lines
     */
    public List<FilteredLine> filterContent(String content) {
        List<FilteredLine> result = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return result;
        }

        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (matchesFilters(lines[i])) {
                result.add(new FilteredLine(i + 1, lines[i]));
            }
        }

        return result;
    }

    /**
     * Get count of active filters
     */
    public int getActiveFilterCount() {
        return (int) rules.stream().filter(FilterRule::isEnabled).count();
    }

    /**
     * Check if any filters are active
     */
    public boolean hasActiveFilters() {
        return getActiveFilterCount() > 0;
    }

    private void loadRules() {
        rules.clear();
        List<FilterRule> loaded;

        if (currentFilePath != null) {
            // Load per-file rules
            loaded = PreferencesManager.loadFilterRules(currentFilePath);
            if (loaded.isEmpty()) {
                // Fall back to global rules if per-file doesn't exist
                List<FilterRule> globalRules = PreferencesManager.loadFilterRules();
                if (!globalRules.isEmpty()) {
                    loaded = globalRules;
                }
            }
        } else {
            // Load global rules if no current file
            loaded = PreferencesManager.loadFilterRules();
        }

        rules.addAll(loaded);
    }

    private void saveRules() {
        List<FilterRule> rulesCopy = new ArrayList<>(rules);

        if (currentFilePath != null) {
            // Save per-file rules
            PreferencesManager.saveFilterRules(currentFilePath, rulesCopy);
        } else {
            // Save global rules if no current file
            PreferencesManager.saveFilterRules(rulesCopy);
        }
    }

    /**
     * Represents a filtered line with original line number and content
     */
    public static class FilteredLine {
        public final int lineNumber;
        public final String content;

        public FilteredLine(int lineNumber, String content) {
            this.lineNumber = lineNumber;
            this.content = content;
        }
    }
}
