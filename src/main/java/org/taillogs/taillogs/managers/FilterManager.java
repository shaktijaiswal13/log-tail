package org.taillogs.taillogs.managers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.taillogs.taillogs.config.PreferencesManager;
import org.taillogs.taillogs.config.ProjectSettings;
import org.taillogs.taillogs.models.FilterRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FilterManager {
    private final ObservableList<FilterRule> rules;
    private String currentFilePath;
    private final Map<String, Boolean> projectEnabledDefaults = new HashMap<>();

    public FilterManager() {
        this.rules = FXCollections.observableArrayList();
    }

    public void setCurrentFile(String filePath) {
        this.currentFilePath = filePath;
        loadRules();
    }

    public void addRule(FilterRule rule) {
        rules.add(rule);
        projectEnabledDefaults.put(rule.getId(), rule.isEnabled());
        saveProjectRules();
        saveRuleStates();
    }

    public void removeRule(String ruleId) {
        rules.removeIf(r -> r.getId().equals(ruleId));
        projectEnabledDefaults.remove(ruleId);
        saveProjectRules();
        saveRuleStates();
    }

    public void toggleRule(String ruleId) {
        rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .ifPresent(r -> {
                    r.setEnabled(!r.isEnabled());
                    saveRuleStates();
                });
    }

    public ObservableList<FilterRule> getRules() {
        return rules;
    }

    public void clearRules() {
        rules.clear();
        projectEnabledDefaults.clear();
        saveProjectRules();
        saveRuleStates();
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
        ProjectSettings projectSettings = PreferencesManager.loadProjectSettings();
        List<FilterRule> loaded = projectSettings.getFilters();

        if (loaded.isEmpty()) {
            List<FilterRule> legacyGlobal = PreferencesManager.loadFilterRules();
            if (!legacyGlobal.isEmpty()) {
                loaded = legacyGlobal;
                projectSettings.setFilters(legacyGlobal);
                PreferencesManager.saveProjectSettings(projectSettings);
            }
        }

        if (currentFilePath != null) {
            List<FilterRule> fileSpecific = PreferencesManager.loadFilterRules(currentFilePath);
            if (!fileSpecific.isEmpty()) {
                loaded = fileSpecific;
            }
        }

        rules.addAll(loaded);

        projectEnabledDefaults.clear();
        for (FilterRule rule : rules) {
            projectEnabledDefaults.put(rule.getId(), rule.isEnabled());
        }

        if (currentFilePath != null) {
            Map<String, Boolean> states = PreferencesManager.loadFilterStates(currentFilePath);

            for (FilterRule rule : rules) {
                if (states.containsKey(rule.getId())) {
                    rule.setEnabled(states.get(rule.getId()));
                } else {
                    rule.setEnabled(projectEnabledDefaults.getOrDefault(rule.getId(), true));
                }
            }
        }
    }

    public void saveProjectRules() {
        List<FilterRule> rulesCopy = new ArrayList<>();
        for (FilterRule rule : rules) {
            FilterRule copy = new FilterRule();
            copy.setId(rule.getId());
            copy.setPattern(rule.getPattern());
            copy.setRegex(rule.isRegex());
            copy.setEnabled(projectEnabledDefaults.getOrDefault(rule.getId(), true));
            rulesCopy.add(copy);
        }

        ProjectSettings projectSettings = new ProjectSettings();
        projectSettings.setFilters(rulesCopy);
        projectSettings.setHighlights(PreferencesManager.loadProjectSettings().getHighlights());
        PreferencesManager.saveProjectSettings(projectSettings);
    }

    public void saveRuleStates() {
        if (currentFilePath == null) {
            return;
        }
        Map<String, Boolean> states = new HashMap<>();
        for (FilterRule rule : rules) {
            states.put(rule.getId(), rule.isEnabled());
        }
        PreferencesManager.saveFilterStates(currentFilePath, states);
    }

    public void reloadForCurrentFile() {
        loadRules();
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    public void applyRulesToCurrentFile(List<FilterRule> newRules) {
        if (currentFilePath == null) {
            return;
        }

        List<FilterRule> normalized = new ArrayList<>();
        if (newRules != null) {
            for (FilterRule src : newRules) {
                if (src == null) {
                    continue;
                }
                FilterRule copy = new FilterRule();
                copy.setId(src.getId() != null ? src.getId() : java.util.UUID.randomUUID().toString());
                copy.setPattern(src.getPattern());
                copy.setRegex(src.isRegex());
                copy.setEnabled(src.isEnabled());
                normalized.add(copy);
            }
        }

        PreferencesManager.saveFilterRules(currentFilePath, normalized);

        Map<String, Boolean> states = new HashMap<>();
        for (FilterRule rule : normalized) {
            states.put(rule.getId(), rule.isEnabled());
        }
        PreferencesManager.saveFilterStates(currentFilePath, states);

        loadRules();
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
