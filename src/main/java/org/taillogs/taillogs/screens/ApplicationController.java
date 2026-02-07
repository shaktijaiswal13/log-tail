package org.taillogs.taillogs.screens;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import org.fxmisc.richtext.CodeArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.Cursor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.taillogs.taillogs.managers.BookmarkManager;
import org.taillogs.taillogs.managers.FilterManager;
import org.taillogs.taillogs.managers.HighlightManager;
import org.taillogs.taillogs.models.Bookmark;
import org.taillogs.taillogs.models.FilterRule;
import org.taillogs.taillogs.models.HighlightPattern;
import org.taillogs.taillogs.utils.FileOperations;
import org.taillogs.taillogs.utils.FileOperations.TailThreadRef;
import org.taillogs.taillogs.utils.FontStylesUtil;
import org.taillogs.taillogs.utils.SyntaxHighlighter;
import org.taillogs.taillogs.config.AppearanceSettings;
import org.taillogs.taillogs.config.PreferencesManager;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationController {
    @FXML
    private Label fileInfoLabel;
    @FXML
    private Button pauseBtn;
    @FXML
    private Button clearBtn;
    @FXML
    private Button refreshBtn;
    @FXML
    private TextField searchField;
    @FXML
    private CodeArea logArea;
    @FXML
    private Label statusLabel;
    @FXML
    private HBox menuBarContainer;
    @FXML
    private HBox tabBar;
    @FXML
    private VBox rightPanelContainer;

    private String currentFilePath;
    private String currentFolderPath;
    private List<String> fileList;
    private List<String> fileListFullPath;
    private TailThreadRef tailThreadRef;
    private boolean pauseMode = false;
    private boolean sidebarVisible = true;
    private String originalLogContent;

    // Multiple open files support
    private ObservableList<String> openFiles;
    private Map<String, String> fileContentCache; // Cache for file contents
    private Map<String, TailThreadRef> fileThreadRefs; // Track tailing threads for each file

    private Runnable onBack;
    private AppearanceSettings appearanceSettings;

    // Managers for highlights, filters, and bookmarks
    private HighlightManager highlightManager;
    private FilterManager filterManager;
    private BookmarkManager bookmarkManager;
    private RightPanelController rightPanelController;

    public void initialize() {
        tailThreadRef = new TailThreadRef();
        fileList = java.util.Collections.emptyList();
        fileListFullPath = java.util.Collections.emptyList();

        // Load appearance settings
        appearanceSettings = PreferencesManager.loadAppearanceSettings();

        // Initialize multiple files support
        openFiles = FXCollections.observableArrayList();
        fileContentCache = new HashMap<>();
        fileThreadRefs = new HashMap<>();

        // Initialize managers
        highlightManager = new HighlightManager();
        filterManager = new FilterManager();
        bookmarkManager = new BookmarkManager();

        // Setup listener for openFiles to update tab bar
        openFiles.addListener((javafx.collections.ListChangeListener<String>) change -> {
            updateTabBar();
        });

        // Setup search field listener
        searchField.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Go to next match on Enter
                goToNextMatch();
            } else {
                filterContent();
            }
        });

        setupUI();
        applyAppearanceSettings(appearanceSettings);

        // Setup right panel immediately (not deferred)
        System.out.println("About to call setupRightPanel()...");
        setupRightPanel();
        System.out.println("setupRightPanel() completed");
    }

    public void applyAppearanceSettings(AppearanceSettings settings) {
        this.appearanceSettings = settings;

        // Apply to log text area
        logArea.setStyle(FontStylesUtil.getLogTextAreaStyle(settings));

        // Apply to search field
        searchField.setStyle(FontStylesUtil.getSearchFieldStyle(settings));

        // Apply to status label
        statusLabel.setStyle(FontStylesUtil.getStatusLabelStyle(settings));

        // Apply to file info label
        fileInfoLabel.setStyle(FontStylesUtil.getLabelStyle(settings, 8));

        // Update button styles
        updateButtonStyles();

        // Update tab bar if needed
        updateTabBar();
    }

    private void updateButtonStyles() {
        pauseBtn.setStyle(FontStylesUtil.getButtonStyle(appearanceSettings, pauseMode));
        clearBtn.setStyle(FontStylesUtil.getButtonStyle(appearanceSettings, false));
        refreshBtn.setStyle(FontStylesUtil.getButtonStyle(appearanceSettings, false));
    }

    private void setupUI() {
        logArea.setWrapText(false);
        logArea.setEditable(false);

        // Configure scrollbars to always be visible
        String scrollbarCSS = "-fx-control-inner-background: #ffffff; " +
                             "-fx-padding: 0; " +
                             "-fx-text-fill: #333333;";
        logArea.setStyle(logArea.getStyle() + " " + scrollbarCSS);

        // Setup hover effects for buttons
        setupButtonHoverEffects();
    }
    
    private void setupButtonHoverEffects() {
        // Buttons now use CSS styling for hover effects to prevent scaling
        // Just update the base colors based on pause state
        updateButtonStyles();
    }


    private void setupRightPanel() {
        System.out.println("=== setupRightPanel() called ===");

        if (rightPanelContainer == null) {
            System.err.println("ERROR: rightPanelContainer is null!");
            return;
        }

        System.out.println("rightPanelContainer is valid, clearing children...");
        rightPanelContainer.getChildren().clear();

        try {
            System.out.println("Creating RightPanelController...");
            rightPanelController = new RightPanelController();

            System.out.println("Creating TabPane...");
            TabPane tabPane = new TabPane();
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            tabPane.setPrefWidth(320);
            tabPane.setMinHeight(200);
            tabPane.setStyle("-fx-background-color: #f5f5f5;");
            VBox.setVgrow(tabPane, Priority.ALWAYS);

            // ===== HIGHLIGHTS TAB =====
            System.out.println("Creating Highlights Tab...");
            VBox highlightsContent = new VBox(8);
            highlightsContent.setStyle("-fx-padding: 8; -fx-background-color: #ffffff;");
            VBox.setVgrow(highlightsContent, Priority.ALWAYS);
            
            Button addHighlightBtn = new Button("+ Add Highlight Pattern");
            addHighlightBtn.setMaxWidth(Double.MAX_VALUE);
            addHighlightBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
            
            ListView<HighlightPattern> highlightsListView = new ListView<>(highlightManager.getPatterns());
            highlightsListView.setMinHeight(100);
            highlightsListView.setPrefHeight(Region.USE_COMPUTED_SIZE);
            highlightsListView.setPlaceholder(new Label("No highlight patterns defined.\nClick the button above to add one."));
            VBox.setVgrow(highlightsListView, Priority.ALWAYS);
            
            highlightsContent.getChildren().addAll(addHighlightBtn, new Separator(), highlightsListView);
            Tab highlightsTab = new Tab("Highlights", highlightsContent);
            highlightsTab.setClosable(false);

            // ===== FILTERS TAB =====
            System.out.println("Creating Filters Tab...");
            VBox filtersContent = new VBox(8);
            filtersContent.setStyle("-fx-padding: 8; -fx-background-color: #ffffff;");
            VBox.setVgrow(filtersContent, Priority.ALWAYS);
            
            Button addFilterBtn = new Button("+ Add Filter");
            addFilterBtn.setMaxWidth(Double.MAX_VALUE);
            addFilterBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");
            
            ListView<FilterRule> filtersListView = new ListView<>(filterManager.getRules());
            filtersListView.setMinHeight(100);
            filtersListView.setPrefHeight(Region.USE_COMPUTED_SIZE);
            filtersListView.setPlaceholder(new Label("No filter rules defined.\nClick the button above to add one."));
            VBox.setVgrow(filtersListView, Priority.ALWAYS);
            
            Button clearFiltersBtn = new Button("Clear All Filters");
            clearFiltersBtn.setMaxWidth(Double.MAX_VALUE);
            clearFiltersBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 6 12;");
            
            filtersContent.getChildren().addAll(addFilterBtn, new Separator(), filtersListView, clearFiltersBtn);
            Tab filtersTab = new Tab("Filters", filtersContent);
            filtersTab.setClosable(false);

            // ===== BOOKMARKS TAB =====
            System.out.println("Creating Bookmarks Tab...");
            VBox bookmarksContent = new VBox(8);
            bookmarksContent.setStyle("-fx-padding: 8; -fx-background-color: #ffffff;");
            VBox.setVgrow(bookmarksContent, Priority.ALWAYS);
            
            Label instructionsLabel = new Label("Click line numbers to bookmark");
            instructionsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 10;");
            instructionsLabel.setWrapText(true);
            
            ListView<Bookmark> bookmarksListView = new ListView<>(bookmarkManager.getBookmarks());
            bookmarksListView.setMinHeight(100);
            bookmarksListView.setPrefHeight(Region.USE_COMPUTED_SIZE);
            bookmarksListView.setPlaceholder(new Label("No bookmarks yet.\nClick on line numbers to add bookmarks."));
            VBox.setVgrow(bookmarksListView, Priority.ALWAYS);
            
            Button clearBookmarksBtn = new Button("Clear All Bookmarks");
            clearBookmarksBtn.setMaxWidth(Double.MAX_VALUE);
            clearBookmarksBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 6 12;");
            
            bookmarksContent.getChildren().addAll(instructionsLabel, new Separator(), bookmarksListView, clearBookmarksBtn);
            Tab bookmarksTab = new Tab("Bookmarks", bookmarksContent);
            bookmarksTab.setClosable(false);

            // Add all tabs to TabPane
            System.out.println("Adding tabs to TabPane...");
            tabPane.getTabs().addAll(highlightsTab, filtersTab, bookmarksTab);
            System.out.println("Tabs added: " + tabPane.getTabs().size());

            // Wire UI components to the RightPanelController
            System.out.println("Wiring components to RightPanelController...");
            rightPanelController.tabPane = tabPane;
            rightPanelController.addHighlightBtn = addHighlightBtn;
            rightPanelController.highlightsListView = highlightsListView;
            rightPanelController.addFilterBtn = addFilterBtn;
            rightPanelController.filtersListView = filtersListView;
            rightPanelController.clearFiltersBtn = clearFiltersBtn;
            rightPanelController.bookmarksListView = bookmarksListView;
            rightPanelController.clearBookmarksBtn = clearBookmarksBtn;

            // Set managers and initialize the controller
            System.out.println("Setting managers and initializing controller...");
            rightPanelController.setManagers(highlightManager, filterManager, bookmarkManager);
            rightPanelController.initialize();

            // Set callbacks for when highlights/filters change
            rightPanelController.setOnHighlightsChanged(this::reapplyHighlighting);
            rightPanelController.setOnFiltersChanged(this::applyFilteringToContent);

            // Add TabPane to the container
            System.out.println("Adding TabPane to rightPanelContainer...");
            rightPanelContainer.getChildren().add(tabPane);
            System.out.println("Container children count: " + rightPanelContainer.getChildren().size());

            // Force layout update
            rightPanelContainer.requestLayout();

            System.out.println("=== Right panel created successfully with 3 tabs ===");

        } catch (Exception e) {
            System.err.println("ERROR creating right panel: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: show error message in the panel
            Label errorLabel = new Label("Error loading right panel:\n" + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 10;");
            errorLabel.setWrapText(true);
            rightPanelContainer.getChildren().add(errorLabel);
        }
    }

    public void setCurrentFile(String filePath) {
        this.currentFilePath = filePath;
        this.currentFolderPath = new File(filePath).getParent();
        populateFiles();

        // Add file to open files list if not already open
        if (!openFiles.contains(filePath)) {
            openFiles.add(filePath);
        }

        // Update bookmark manager to use current file
        bookmarkManager.setCurrentFile(filePath);

        loadCurrentFile();
    }

    public void setCurrentFolder(String folderPath) {
        this.currentFolderPath = folderPath;
        populateFiles();
        if (!fileListFullPath.isEmpty()) {
            currentFilePath = fileListFullPath.get(0);
            // Add first file to open files
            if (!openFiles.contains(currentFilePath)) {
                openFiles.add(currentFilePath);
            }
            // Update bookmark manager to use current file
            bookmarkManager.setCurrentFile(currentFilePath);
            loadCurrentFile();
        }
    }

    private void populateFiles() {
        if (currentFolderPath == null) {
            fileList = java.util.Collections.emptyList();
            fileListFullPath = java.util.Collections.emptyList();
            return;
        }

        fileListFullPath = FileOperations.getLogFilesFullPath(currentFolderPath);
        fileList = java.util.Collections.emptyList();

        if (!fileListFullPath.isEmpty()) {
            fileList = fileListFullPath.stream()
                    .map(path -> new File(path).getName())
                    .toList();
        }

    }


    private void loadCurrentFile() {
        if (currentFilePath != null && new File(currentFilePath).exists()) {
            // Stop all previous tailing threads for other files
            for (TailThreadRef threadRef : fileThreadRefs.values()) {
                threadRef.setActive(false);
            }
            fileThreadRefs.clear();

            // Stop main tailing thread
            tailThreadRef.setActive(false);

            // Create a new thread reference for this file
            TailThreadRef fileThreadRef = new TailThreadRef();
            fileThreadRefs.put(currentFilePath, fileThreadRef);

            logArea.clear();
            long fileSize = FileOperations.loadFileContent(logArea, currentFilePath);
            // Initialize file position to current file size so tailing starts from here
            fileThreadRef.setFilePosition(fileSize);
            fileInfoLabel.setText("Ready");

            // Start tailing with the file-specific thread ref
            FileOperations.startTailing(currentFilePath, logArea, fileThreadRef);
            statusLabel.setText("Tailing: " + new File(currentFilePath).getName());
            originalLogContent = logArea.getText();
        }
    }

    @FXML
    protected void onTogglePause() {
        pauseMode = !pauseMode;
        if (pauseMode) {
            // Stop tailing for current file only
            if (fileThreadRefs.containsKey(currentFilePath)) {
                fileThreadRefs.get(currentFilePath).setActive(false);
            }
            pauseBtn.setStyle(FontStylesUtil.getButtonStyle(appearanceSettings, true));
            statusLabel.setText("Paused");
        } else {
            pauseBtn.setStyle(FontStylesUtil.getButtonStyle(appearanceSettings, false));
            if (currentFilePath != null) {
                // Create new thread ref if needed
                if (!fileThreadRefs.containsKey(currentFilePath)) {
                    fileThreadRefs.put(currentFilePath, new TailThreadRef());
                }
                FileOperations.startTailing(currentFilePath, logArea, fileThreadRefs.get(currentFilePath));
                statusLabel.setText("Tailing...");
            }
        }
    }

    @FXML
    protected void onClear() {
        logArea.clear();
        statusLabel.setText("Cleared");
    }

    @FXML
    protected void onRefresh() {
        if (currentFilePath != null && new File(currentFilePath).exists()) {
            FileOperations.refreshFile(logArea, currentFilePath);
            statusLabel.setText("Refreshed");
            originalLogContent = logArea.getText();
        }
    }

    private int currentMatchIndex = 0;
    private List<Integer> matchPositions = new ArrayList<>();
    private String currentSearchTerm = "";
    private boolean wasAutoTailingBeforeSearch = false;

    private void goToNextMatch() {
        if (matchPositions.isEmpty()) {
            return;
        }

        // Move to next match (cycle back to first if at the end)
        currentMatchIndex = (currentMatchIndex + 1) % matchPositions.size();
        int matchPos = matchPositions.get(currentMatchIndex);

        // Pause tailing if it was active
        if (currentFilePath != null && fileThreadRefs.containsKey(currentFilePath)) {
            wasAutoTailingBeforeSearch = !pauseMode;
            if (wasAutoTailingBeforeSearch) {
                fileThreadRefs.get(currentFilePath).setActive(false);
                pauseMode = true;
                pauseBtn.setStyle(FontStylesUtil.getButtonStyle(appearanceSettings, true));
                statusLabel.setText("Paused for search...");
            }
        }

        // Move caret to match and scroll to it
        logArea.moveTo(matchPos);
        logArea.requestFollowCaret();

        // Reapply highlighting to show current match
        applySearchHighlighting();

        statusLabel.setText("Match " + (currentMatchIndex + 1) + " of " + matchPositions.size());

        // Resume tailing after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    if (wasAutoTailingBeforeSearch && currentFilePath != null && !pauseMode) {
                        // Only resume if user hasn't manually changed state
                        if (fileThreadRefs.containsKey(currentFilePath)) {
                            fileThreadRefs.get(currentFilePath).setActive(true);
                            FileOperations.startTailing(currentFilePath, logArea, fileThreadRefs.get(currentFilePath));
                            pauseMode = false;
                            pauseBtn.setStyle(FontStylesUtil.getButtonStyle(appearanceSettings, false));
                            statusLabel.setText("Tailing...");
                        }
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void filterContent() {
        String searchTerm = searchField.getText().trim();

        if (searchTerm.isEmpty()) {
            // Remove all highlighting when search is cleared
            clearSearchHighlights();
            currentSearchTerm = "";
            statusLabel.setText("Ready");
            matchPositions.clear();
            currentMatchIndex = 0;
            return;
        }

        // Get current content (don't clear it)
        String content = logArea.getText();
        if (content.isEmpty()) {
            return;
        }

        // Find all occurrences (case-insensitive search)
        String contentLower = content.toLowerCase();
        String searchTermLower = searchTerm.toLowerCase();
        matchPositions.clear();
        currentMatchIndex = 0;
        int startIndex = 0;

        while ((startIndex = contentLower.indexOf(searchTermLower, startIndex)) != -1) {
            matchPositions.add(startIndex);
            startIndex += searchTerm.length();
        }

        currentSearchTerm = searchTerm;

        if (!matchPositions.isEmpty()) {
            // Pause tailing if it was active
            if (currentFilePath != null && fileThreadRefs.containsKey(currentFilePath)) {
                wasAutoTailingBeforeSearch = !pauseMode;
                if (wasAutoTailingBeforeSearch) {
                    fileThreadRefs.get(currentFilePath).setActive(false);
                    pauseMode = true;
                    pauseBtn.setStyle(FontStylesUtil.getButtonStyle(appearanceSettings, true));
                }
            }

            // Apply highlighting to all matches
            applySearchHighlighting();

            // Scroll to first match
            int firstMatchPos = matchPositions.get(0);
            logArea.moveTo(firstMatchPos);
            logArea.requestFollowCaret();

            // Update status
            statusLabel.setText("Found " + matchPositions.size() + " match" + (matchPositions.size() == 1 ? "" : "es"));

            // Resume tailing after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> {
                        if (wasAutoTailingBeforeSearch && currentFilePath != null && !pauseMode) {
                            // Only resume if user hasn't manually changed state
                            if (fileThreadRefs.containsKey(currentFilePath)) {
                                fileThreadRefs.get(currentFilePath).setActive(true);
                                FileOperations.startTailing(currentFilePath, logArea, fileThreadRefs.get(currentFilePath));
                                pauseMode = false;
                                pauseBtn.setStyle(FontStylesUtil.getButtonStyle(appearanceSettings, false));
                                statusLabel.setText("Tailing...");
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else {
            // No matches found
            clearSearchHighlights();
            statusLabel.setText("No matches found");
        }
    }

    private void applySearchHighlighting() {
        if (currentSearchTerm.isEmpty() || matchPositions.isEmpty()) {
            return;
        }

        String content = logArea.getText();
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        // Sort match positions for easier iteration
        for (int pos : matchPositions) {
            // Add unstyled content before this match
            if (pos > lastEnd) {
                spansBuilder.add(Collections.emptyList(), pos - lastEnd);
            }

            // Determine if this is the current match
            String styleClass = (matchPositions.indexOf(pos) == currentMatchIndex) ? "search-current" : "search-result";
            spansBuilder.add(Collections.singleton(styleClass), currentSearchTerm.length());
            lastEnd = pos + currentSearchTerm.length();
        }

        // Add remaining content
        if (lastEnd < content.length()) {
            spansBuilder.add(Collections.emptyList(), content.length() - lastEnd);
        }

        StyleSpans<Collection<String>> spans = spansBuilder.create();
        logArea.setStyleSpans(0, spans);
    }

    private void clearSearchHighlights() {
        if (logArea.getText().isEmpty()) {
            return;
        }
        // Reapply syntax highlighting without search highlighting
        SyntaxHighlighter.applyLogLevelHighlighting(logArea);
    }

    // Setter for onBack callback
    public void setOnBack(Runnable callback) {
        this.onBack = callback;
    }

    public void stopTailing() {
        tailThreadRef.setActive(false);
        // Stop all tailing threads for open files
        for (TailThreadRef threadRef : fileThreadRefs.values()) {
            threadRef.setActive(false);
        }
    }

    // Menu bar operations
    public void setMenuBar(MenuBar menuBar) {
        try {
            if (menuBarContainer != null) {
                // Don't override the menu bar styling - keep the bright blue theme
                menuBarContainer.getChildren().clear();
                menuBarContainer.getChildren().add(menuBar);
                HBox.setHgrow(menuBar, Priority.ALWAYS);
            }
        } catch (Exception e) {
            System.err.println("Error setting menu bar: " + e.getMessage());
        }
    }

    public void clearDisplay() {
        onClear();
    }

    public void refreshDisplay() {
        onRefresh();
    }

    public void togglePause() {
        onTogglePause();
    }

    // Multiple open files management
    private void closeFile(String filePath) {
        // Stop tailing for this file
        if (fileThreadRefs.containsKey(filePath)) {
            TailThreadRef threadRef = fileThreadRefs.get(filePath);
            threadRef.setActive(false);
            fileThreadRefs.remove(filePath);
        }

        // Remove from cache
        fileContentCache.remove(filePath);

        // Store index before removal
        int removedIndex = openFiles.indexOf(filePath);

        // Remove from open files list
        openFiles.remove(filePath);

        // If closing current file, switch to another
        if (filePath.equals(currentFilePath)) {
            if (!openFiles.isEmpty()) {
                // Select the file that was after the removed one, or the last one
                int newIndex = Math.min(removedIndex, openFiles.size() - 1);
                currentFilePath = openFiles.get(newIndex);
                bookmarkManager.setCurrentFile(currentFilePath);
                loadCurrentFile();
            } else {
                currentFilePath = null;
                logArea.clear();
                fileInfoLabel.setText("Ready");
                statusLabel.setText("No files open");
            }
        }
    }

    /**
     * Reapply highlighting when custom patterns change
     */
    private void reapplyHighlighting() {
        if (logArea.getText().isEmpty()) {
            return;
        }

        if (currentSearchTerm.isEmpty() || matchPositions.isEmpty()) {
            // No search active, apply combined highlighting
            highlightManager.applyCombinedHighlighting(logArea);
        } else {
            // Search is active, keep search highlighting but apply combined as base
            applySearchHighlighting();
        }

        if (rightPanelController != null) {
            rightPanelController.refreshHighlights();
        }
    }

    /**
     * Apply filtering when filter rules change
     */
    private void applyFilteringToContent() {
        if (logArea.getText().isEmpty()) {
            return;
        }

        if (filterManager.hasActiveFilters()) {
            // Apply filters to original content
            List<FilterManager.FilteredLine> filtered = filterManager.filterContent(originalLogContent);

            // Build filtered display
            StringBuilder filteredContent = new StringBuilder();
            for (FilterManager.FilteredLine line : filtered) {
                filteredContent.append(line.content).append("\n");
            }

            logArea.clear();
            logArea.appendText(filteredContent.toString());
            highlightManager.applyCombinedHighlighting(logArea);
            statusLabel.setText("Showing " + filtered.size() + " of " + originalLogContent.split("\n", -1).length + " lines");
        } else {
            // No filters, show original content
            logArea.clear();
            logArea.appendText(originalLogContent);
            highlightManager.applyCombinedHighlighting(logArea);
            statusLabel.setText("Ready");
        }

        if (rightPanelController != null) {
            rightPanelController.refreshFilters();
        }
    }


    // Update tab bar with open files as tabs
    private void updateTabBar() {
        Platform.runLater(() -> {
            tabBar.getChildren().clear();

            for (String filePath : openFiles) {
                HBox tabContainer = createTab(filePath);
                tabBar.getChildren().add(tabContainer);
            }
        });
    }

    // Create a single tab for a file
    private HBox createTab(String filePath) {
        HBox tab = new HBox(4);
        tab.setStyle(
            "-fx-padding: 6 8 6 8; " +
            "-fx-background-color: #e8e8e8; " +
            "-fx-border-color: #cccccc; " +
            "-fx-border-width: 0 1 1 0; " +
            "-fx-alignment: CENTER_LEFT;"
        );
        tab.setCursor(javafx.scene.Cursor.HAND);

        // File name label
        Label fileName = new Label(new File(filePath).getName());
        int tabFontSize = Math.max(8, appearanceSettings.getFontSize() - 4);
        String tabFontWeightStyle = appearanceSettings.getFontWeight().equals("Bold") ? "-fx-font-weight: bold; " : "";
        fileName.setStyle("-fx-font-size: " + tabFontSize + "; -fx-text-fill: #333333; " + tabFontWeightStyle);

        // Close button
        Button closeTab = new Button("✕");
        closeTab.setStyle(
            "-fx-padding: 2 4 2 4; " +
            "-fx-font-size: 8; " +
            "-fx-background-color: white; " +
            "-fx-text-fill: black; " +
            "-fx-border-width: 0; " +
            "-fx-border-radius: 0; " +
            "-fx-min-width: 20; " +
            "-fx-min-height: 18;"
        );

        closeTab.setOnAction(event -> closeFile(filePath));

        // On tab click, switch to this file
        tab.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && !event.getTarget().equals(closeTab)) {
                currentFilePath = filePath;
                tailThreadRef.setActive(false);
                pauseMode = false;
                pauseBtn.setStyle("-fx-padding: 3 8 3 8; -fx-font-size: 9; -fx-background-color: #e0e0e0; -fx-text-fill: black; -fx-border-color: #333333; -fx-border-width: 1; -fx-border-radius: 0;");
                loadCurrentFile();
                updateTabBar();
            }
        });

        tab.getChildren().addAll(fileName, closeTab);

        // Highlight active tab
        if (filePath.equals(currentFilePath)) {
            tab.setStyle(
                "-fx-padding: 6 8 6 8; " +
                "-fx-background-color: #ffffff; " +
                "-fx-border-color: #2196F3 #cccccc #ffffff #cccccc; " +
                "-fx-border-width: 2 1 0 1; " +
                "-fx-alignment: CENTER_LEFT;"
            );
            fileName.setStyle("-fx-font-size: 9; -fx-font-weight: bold; -fx-text-fill: #2196F3;");
        }

        return tab;
    }
}
