package org.taillogs.taillogs.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.Cursor;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.taillogs.taillogs.managers.BookmarkManager;
import org.taillogs.taillogs.managers.FilterManager;
import org.taillogs.taillogs.managers.HighlightManager;
import org.taillogs.taillogs.config.PreferencesManager;
import org.taillogs.taillogs.models.Bookmark;
import org.taillogs.taillogs.models.FilterRule;
import org.taillogs.taillogs.models.HighlightPattern;
import org.taillogs.taillogs.models.SavedSettingsProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RightPanelController {
    public TabPane tabPane;
    public Button addHighlightBtn;
    public Button loadHighlightsBtn;
    public Button saveHighlightsBtn;
    public ListView<HighlightPattern> highlightsListView;
    public Button addFilterBtn;
    public Button loadFiltersBtn;
    public ListView<FilterRule> filtersListView;
    public Button clearFiltersBtn;
    public Button saveFiltersBtn;
    public ListView<Bookmark> bookmarksListView;
    public Button clearBookmarksBtn;

    private HighlightManager highlightManager;
    private FilterManager filterManager;
    private BookmarkManager bookmarkManager;
    private Runnable onHighlightsChanged;
    private Runnable onFiltersChanged;

    public void initialize() {
        System.out.println("[RightPanelController] initialize() called");
        setupHighlightsTab();
        setupFiltersTab();
        setupBookmarksTab();
        System.out.println("[RightPanelController] initialize() completed");
    }

    public void setManagers(HighlightManager highlightManager, FilterManager filterManager, BookmarkManager bookmarkManager) {
        System.out.println("[RightPanelController] setManagers() called");
        this.highlightManager = highlightManager;
        this.filterManager = filterManager;
        this.bookmarkManager = bookmarkManager;

        updateHighlightsList();
        updateFiltersList();
        updateBookmarksList();
    }

    public void setOnHighlightsChanged(Runnable callback) {
        System.out.println("[RightPanelController] setOnHighlightsChanged() callback registered: " + (callback != null));
        this.onHighlightsChanged = callback;
    }

    public void setOnFiltersChanged(Runnable callback) {
        this.onFiltersChanged = callback;
    }

    // ========== HIGHLIGHTS TAB ==========

    private void setupHighlightsTab() {
        addHighlightBtn.setOnAction(e -> showAddHighlightDialog());
        if (loadHighlightsBtn != null) {
            loadHighlightsBtn.setOnAction(e -> showLoadSettingsDialog());
        }
        if (saveHighlightsBtn != null) {
            saveHighlightsBtn.setOnAction(e -> showSaveSettingsDialog());
        }
        highlightsListView.setCellFactory(this::createHighlightCell);
        setupSaveHighlightsVisibility();
    }

    private void updateHighlightsList() {
        highlightsListView.setItems(highlightManager.getPatterns());
    }

    private ListCell<HighlightPattern> createHighlightCell(ListView<HighlightPattern> list) {
        return new ListCell<HighlightPattern>() {
            @Override
            protected void updateItem(HighlightPattern pattern, boolean empty) {
                super.updateItem(pattern, empty);
                if (empty || pattern == null) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(8);
                    hbox.setStyle("-fx-padding: 4;");
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    // Checkbox for enabled/disabled
                    CheckBox checkbox = new CheckBox();
                    checkbox.setSelected(pattern.isEnabled());
                    checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        pattern.setEnabled(newVal);
                        highlightManager.savePatternStates();
                        System.out.println("[RightPanelController] Pattern toggled: " + pattern.getPattern() + " enabled=" + newVal);
                        if (onHighlightsChanged != null) {
                            System.out.println("[RightPanelController] Calling onHighlightsChanged callback");
                            onHighlightsChanged.run();
                        }
                    });

                    // Color indicator
                    ColorPicker colorPicker = new ColorPicker(Color.web(pattern.getColor()));
                    colorPicker.setPrefWidth(50);
                    colorPicker.setOnAction(e -> {
                        String newColor = colorPicker.getValue().toString().replace("0x", "#");
                        System.out.println("[RightPanelController] Color changed for pattern: " + pattern.getPattern() + " to " + newColor);
                        pattern.setColor(newColor);
                        highlightManager.updatePattern(pattern);
                        if (onHighlightsChanged != null) {
                            System.out.println("[RightPanelController] Calling onHighlightsChanged callback");
                            onHighlightsChanged.run();
                        }
                    });

                    // Pattern text
                    Label patternLabel = new Label(pattern.getPattern());
                    patternLabel.setStyle("-fx-text-fill: #333;");
                    patternLabel.setWrapText(false);
                    HBox.setHgrow(patternLabel, Priority.ALWAYS);

                    // Delete button
                    Button deleteBtn = new Button("x");
                    deleteBtn.setPrefWidth(30);
                    deleteBtn.setStyle("-fx-font-size: 14; -fx-cursor: hand;");
                    deleteBtn.setCursor(Cursor.HAND);
                    deleteBtn.setOnAction(e -> {
                        System.out.println("[RightPanelController] Deleting pattern: " + pattern.getPattern());
                        highlightManager.removePattern(pattern.getId());
                        if (onHighlightsChanged != null) {
                            System.out.println("[RightPanelController] Calling onHighlightsChanged callback");
                            onHighlightsChanged.run();
                        }
                    });

                    hbox.getChildren().addAll(checkbox, colorPicker, patternLabel, deleteBtn);
                    setGraphic(hbox);
                }
            }
        };
    }

    private void showAddHighlightDialog() {
        System.out.println("[RightPanelController] showAddHighlightDialog() called");
        
        Dialog<HighlightPattern> dialog = new Dialog<>();
        dialog.setTitle("Add Highlight Pattern");
        dialog.setHeaderText("Create a new highlight pattern");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField patternField = new TextField();
        patternField.setPromptText("Enter text or regex pattern");
        CheckBox isRegexCheckbox = new CheckBox("Regex Pattern");
        ColorPicker colorPicker = new ColorPicker(Color.RED);

        grid.add(new Label("Pattern:"), 0, 0);
        grid.add(patternField, 1, 0);
        grid.add(isRegexCheckbox, 1, 1);
        grid.add(new Label("Color:"), 0, 2);
        grid.add(colorPicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK && !patternField.getText().isEmpty()) {
                String colorStr = colorPicker.getValue().toString().replace("0x", "#");
                System.out.println("[RightPanelController] Creating pattern: " + patternField.getText() + " with color: " + colorStr);
                HighlightPattern pattern = new HighlightPattern(
                        patternField.getText(),
                        colorStr,
                        isRegexCheckbox.isSelected()
                );
                return pattern;
            }
            return null;
        });

        Optional<HighlightPattern> result = dialog.showAndWait();
        result.ifPresent(pattern -> {
            System.out.println("[RightPanelController] Pattern created: " + pattern);
            highlightManager.addPattern(pattern);
            System.out.println("[RightPanelController] Pattern added to highlightManager, patterns count: " + highlightManager.getPatterns().size());
            
            if (onHighlightsChanged != null) {
                System.out.println("[RightPanelController] Calling onHighlightsChanged callback...");
                onHighlightsChanged.run();
                System.out.println("[RightPanelController] onHighlightsChanged callback completed");
            } else {
                System.err.println("[RightPanelController] WARNING: onHighlightsChanged callback is null!");
            }
        });
    }

    // ========== FILTERS TAB ==========

    private void setupFiltersTab() {
        addFilterBtn.setOnAction(e -> showAddFilterDialog());
        clearFiltersBtn.setOnAction(e -> {
            filterManager.clearRules();
            if (onFiltersChanged != null) onFiltersChanged.run();
        });
        if (loadFiltersBtn != null) {
            loadFiltersBtn.setOnAction(e -> showLoadSettingsDialog());
        }
        if (saveFiltersBtn != null) {
            saveFiltersBtn.setOnAction(e -> showSaveSettingsDialog());
        }
        filtersListView.setCellFactory(this::createFilterCell);
        setupSaveFiltersVisibility();
    }

    private void updateFiltersList() {
        filtersListView.setItems(filterManager.getRules());
    }

    private void setupSaveHighlightsVisibility() {
        if (saveHighlightsBtn == null || highlightManager == null) {
            return;
        }
        updateSaveHighlightsVisibility();
        highlightManager.getPatterns().addListener((javafx.collections.ListChangeListener<HighlightPattern>) change -> updateSaveHighlightsVisibility());
    }

    private void updateSaveHighlightsVisibility() {
        if (highlightManager == null) {
            return;
        }
        boolean hasItems = !highlightManager.getPatterns().isEmpty();
        saveHighlightsBtn.setVisible(hasItems);
        saveHighlightsBtn.setManaged(hasItems);
    }

    private void setupSaveFiltersVisibility() {
        if (saveFiltersBtn == null || filterManager == null) {
            return;
        }
        updateSaveFiltersVisibility();
        filterManager.getRules().addListener((javafx.collections.ListChangeListener<FilterRule>) change -> updateSaveFiltersVisibility());
    }

    private void updateSaveFiltersVisibility() {
        if (filterManager == null) {
            return;
        }
        boolean hasItems = !filterManager.getRules().isEmpty();
        saveFiltersBtn.setVisible(hasItems);
        saveFiltersBtn.setManaged(hasItems);
    }

    private void reloadHighlights() {
        if (highlightManager == null) {
            return;
        }
        highlightManager.reloadForCurrentFile();
        updateHighlightsList();
        highlightsListView.refresh();
        if (onHighlightsChanged != null) {
            onHighlightsChanged.run();
        }
    }

    private void reloadFilters() {
        if (filterManager == null) {
            return;
        }
        filterManager.reloadForCurrentFile();
        updateFiltersList();
        filtersListView.refresh();
        if (onFiltersChanged != null) {
            onFiltersChanged.run();
        }
    }

    private void showSaveSettingsDialog() {
        if (highlightManager == null || filterManager == null) {
            return;
        }
        if (highlightManager.getCurrentFilePath() == null) {
            showInfo("No open file", "Open a file before saving settings.");
            return;
        }

        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Save Settings");
        nameDialog.setHeaderText("Save highlight/filter settings");
        nameDialog.setContentText("Settings name:");

        Optional<String> result = nameDialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String name = result.get().trim();
        if (name.isEmpty()) {
            showInfo("Invalid name", "Settings name cannot be empty.");
            return;
        }

        List<HighlightPattern> highlightsCopy = copyHighlights(highlightManager.getPatterns());
        List<FilterRule> filtersCopy = copyFilters(filterManager.getRules());
        PreferencesManager.saveNamedSettings(name, highlightsCopy, filtersCopy);
        showInfo("Saved", "Settings saved as \"" + name + "\".");
    }

    private void showLoadSettingsDialog() {
        if (highlightManager == null || filterManager == null) {
            return;
        }
        if (highlightManager.getCurrentFilePath() == null || filterManager.getCurrentFilePath() == null) {
            showInfo("No open file", "Open a file before loading settings.");
            return;
        }

        List<String> savedNames = PreferencesManager.listNamedSettings();
        if (savedNames.isEmpty()) {
            showInfo("No saved settings", "No settings found in ~/.tail_logs/settings.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Load Settings");
        dialog.setHeaderText("Select a saved settings profile");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(savedNames);
        listView.setPrefHeight(320);
        listView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label nameLabel = new Label(item);
                    HBox.setHgrow(nameLabel, Priority.ALWAYS);

                    Button applyBtn = new Button("Apply");
                    applyBtn.setCursor(Cursor.HAND);
                    applyBtn.setOnAction(e -> applySavedSettings(item, dialog));

                    row.getChildren().addAll(nameLabel, applyBtn);
                    setGraphic(row);
                }
            }
        });

        dialog.getDialogPane().setContent(listView);
        dialog.showAndWait();
    }

    private void applySavedSettings(String settingsName, Dialog<Void> parentDialog) {
        SavedSettingsProfile profile = PreferencesManager.loadNamedSettings(settingsName);
        if (profile == null) {
            showInfo("Load failed", "Unable to load settings: " + settingsName);
            return;
        }

        highlightManager.applyPatternsToCurrentFile(profile.getHighlights());
        filterManager.applyRulesToCurrentFile(profile.getFilters());

        updateHighlightsList();
        highlightsListView.refresh();
        updateFiltersList();
        filtersListView.refresh();

        if (onHighlightsChanged != null) {
            onHighlightsChanged.run();
        }
        if (onFiltersChanged != null) {
            onFiltersChanged.run();
        }

        if (parentDialog != null) {
            parentDialog.close();
        }
        showInfo("Applied", "Applied \"" + settingsName + "\" to current file.");
    }

    private List<HighlightPattern> copyHighlights(List<HighlightPattern> source) {
        List<HighlightPattern> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (HighlightPattern pattern : source) {
            HighlightPattern copy = new HighlightPattern();
            copy.setId(pattern.getId());
            copy.setPattern(pattern.getPattern());
            copy.setColor(pattern.getColor());
            copy.setRegex(pattern.isRegex());
            copy.setEnabled(pattern.isEnabled());
            result.add(copy);
        }
        return result;
    }

    private List<FilterRule> copyFilters(List<FilterRule> source) {
        List<FilterRule> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (FilterRule rule : source) {
            FilterRule copy = new FilterRule();
            copy.setId(rule.getId());
            copy.setPattern(rule.getPattern());
            copy.setRegex(rule.isRegex());
            copy.setEnabled(rule.isEnabled());
            result.add(copy);
        }
        return result;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private ListCell<FilterRule> createFilterCell(ListView<FilterRule> list) {
        return new ListCell<FilterRule>() {
            @Override
            protected void updateItem(FilterRule rule, boolean empty) {
                super.updateItem(rule, empty);
                if (empty || rule == null) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(8);
                    hbox.setStyle("-fx-padding: 4;");
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    // Checkbox for enabled/disabled
                    CheckBox checkbox = new CheckBox();
                    checkbox.setSelected(rule.isEnabled());
                    checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        rule.setEnabled(newVal);
                        filterManager.saveRuleStates();
                        System.out.println("[RightPanelController] Rule toggled: " + rule.getPattern() + " enabled=" + newVal);
                        if (onFiltersChanged != null) onFiltersChanged.run();
                    });

                    // Rule text
                    Label ruleLabel = new Label(rule.getPattern());
                    ruleLabel.setStyle("-fx-text-fill: #333;");
                    ruleLabel.setWrapText(false);
                    HBox.setHgrow(ruleLabel, Priority.ALWAYS);

                    // Delete button
                    Button deleteBtn = new Button("x");
                    deleteBtn.setPrefWidth(30);
                    deleteBtn.setStyle("-fx-font-size: 14; -fx-cursor: hand;");
                    deleteBtn.setCursor(Cursor.HAND);
                    deleteBtn.setOnAction(e -> {
                        filterManager.removeRule(rule.getId());
                        if (onFiltersChanged != null) onFiltersChanged.run();
                    });

                    hbox.getChildren().addAll(checkbox, ruleLabel, deleteBtn);
                    setGraphic(hbox);
                }
            }
        };
    }

    private void showAddFilterDialog() {
        Dialog<FilterRule> dialog = new Dialog<>();
        dialog.setTitle("Add Filter");
        dialog.setHeaderText("Create a new filter rule");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField patternField = new TextField();
        patternField.setPromptText("Enter text or regex pattern");
        CheckBox isRegexCheckbox = new CheckBox("Regex Pattern");

        grid.add(new Label("Pattern:"), 0, 0);
        grid.add(patternField, 1, 0);
        grid.add(isRegexCheckbox, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK && !patternField.getText().isEmpty()) {
                return new FilterRule(patternField.getText(), isRegexCheckbox.isSelected());
            }
            return null;
        });

        Optional<FilterRule> result = dialog.showAndWait();
        result.ifPresent(rule -> {
            filterManager.addRule(rule);
            if (onFiltersChanged != null) onFiltersChanged.run();
        });
    }

    // ========== BOOKMARKS TAB ==========

    private void setupBookmarksTab() {
        clearBookmarksBtn.setOnAction(e -> bookmarkManager.clearBookmarks());
        bookmarksListView.setCellFactory(this::createBookmarkCell);
    }

    private void updateBookmarksList() {
        bookmarksListView.setItems(bookmarkManager.getBookmarks());
    }

    private ListCell<Bookmark> createBookmarkCell(ListView<Bookmark> list) {
        return new ListCell<Bookmark>() {
            @Override
            protected void updateItem(Bookmark bookmark, boolean empty) {
                super.updateItem(bookmark, empty);
                if (empty || bookmark == null) {
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(2);
                    vbox.setStyle("-fx-padding: 6;");

                    Label lineLabel = new Label("Line " + bookmark.getLineNumber());
                    lineLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

                    Label previewLabel = new Label(bookmark.getLinePreview());
                    previewLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 9;");
                    previewLabel.setWrapText(true);

                    HBox actionBox = new HBox(4);
                    actionBox.setAlignment(Pos.CENTER_LEFT);

                    Button goBtn = new Button("Go");
                    goBtn.setPrefWidth(40);
                    goBtn.setStyle("-fx-font-size: 10; -fx-cursor: hand;");
                    goBtn.setCursor(Cursor.HAND);
                    goBtn.setOnAction(e -> {
                        // Navigate to bookmark (to be implemented by ApplicationController)
                    });

                    Button deleteBtn = new Button("x");
                    deleteBtn.setPrefWidth(30);
                    deleteBtn.setStyle("-fx-font-size: 12; -fx-cursor: hand;");
                    deleteBtn.setCursor(Cursor.HAND);
                    deleteBtn.setOnAction(e -> bookmarkManager.removeBookmark(bookmark.getId()));

                    actionBox.getChildren().addAll(goBtn, deleteBtn);

                    vbox.getChildren().addAll(lineLabel, previewLabel, actionBox);
                    setGraphic(vbox);
                }
            }
        };
    }

    public void refreshHighlights() {
        highlightsListView.refresh();
    }

    public void refreshFilters() {
        filtersListView.refresh();
    }

    public void refreshBookmarks() {
        bookmarksListView.refresh();
    }
}
