package org.taillogs.taillogs.screens;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

import org.taillogs.taillogs.utils.FileOperations;
import org.taillogs.taillogs.utils.FileOperations.TailThreadRef;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationController {
    @FXML
    private Button toggleFilesBtn;
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
    private TextArea logTextArea;
    @FXML
    private Label statusLabel;
    @FXML
    private HBox menuBarContainer;
    @FXML
    private HBox tabBar;

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

    public void initialize() {
        tailThreadRef = new TailThreadRef();
        fileList = java.util.Collections.emptyList();
        fileListFullPath = java.util.Collections.emptyList();

        // Initialize multiple files support
        openFiles = FXCollections.observableArrayList();
        fileContentCache = new HashMap<>();
        fileThreadRefs = new HashMap<>();

        // Setup listener for openFiles to update tab bar
        openFiles.addListener((javafx.collections.ListChangeListener<String>) change -> {
            updateTabBar();
        });

        // Setup search field listener
        searchField.setOnKeyReleased(event -> filterContent());

        setupUI();
    }

    private void setupUI() {
        logTextArea.setWrapText(false);
        logTextArea.setEditable(false);
    }

    public void setCurrentFile(String filePath) {
        this.currentFilePath = filePath;
        this.currentFolderPath = new File(filePath).getParent();
        populateFiles();

        // Add file to open files list if not already open
        if (!openFiles.contains(filePath)) {
            openFiles.add(filePath);
        }

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

            logTextArea.clear();
            FileOperations.loadFileContent(logTextArea, currentFilePath);
            fileInfoLabel.setText("📄 " + new File(currentFilePath).getName());

            // Start tailing with the file-specific thread ref
            FileOperations.startTailing(currentFilePath, logTextArea, fileThreadRef);
            statusLabel.setText("Tailing: " + new File(currentFilePath).getName());
            originalLogContent = logTextArea.getText();
        }
    }

    @FXML
    protected void onToggleSidebar() {
        // Toggle sidebar visibility (for future implementation)
    }

    @FXML
    protected void onTogglePause() {
        pauseMode = !pauseMode;
        if (pauseMode) {
            // Stop tailing for current file only
            if (fileThreadRefs.containsKey(currentFilePath)) {
                fileThreadRefs.get(currentFilePath).setActive(false);
            }
            pauseBtn.setText("▶ Resume");
            pauseBtn.setStyle("-fx-padding: 5 10 5 10; -fx-font-size: 9; -fx-font-weight: bold; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-border-radius: 0;");
            statusLabel.setText("Paused");
        } else {
            pauseBtn.setText("⏸ Pause");
            pauseBtn.setStyle("-fx-padding: 5 10 5 10; -fx-font-size: 9; -fx-font-weight: bold; -fx-background-color: #FF9800; -fx-text-fill: white; -fx-border-radius: 0;");
            if (currentFilePath != null) {
                // Create new thread ref if needed
                if (!fileThreadRefs.containsKey(currentFilePath)) {
                    fileThreadRefs.put(currentFilePath, new TailThreadRef());
                }
                FileOperations.startTailing(currentFilePath, logTextArea, fileThreadRefs.get(currentFilePath));
                statusLabel.setText("Resuming...");
            }
        }
    }

    @FXML
    protected void onClear() {
        logTextArea.clear();
        statusLabel.setText("Cleared");
    }

    @FXML
    protected void onRefresh() {
        if (currentFilePath != null && new File(currentFilePath).exists()) {
            FileOperations.refreshFile(logTextArea, currentFilePath);
            statusLabel.setText("Refreshed");
            originalLogContent = logTextArea.getText();
        }
    }

    private void filterContent() {
        String searchTerm = searchField.getText().trim();

        if (searchTerm.isEmpty()) {
            // Restore original content if search is cleared
            logTextArea.clear();
            logTextArea.appendText(originalLogContent);
            return;
        }

        // Display original content with highlighting
        logTextArea.clear();
        logTextArea.appendText(originalLogContent);

        // Find and highlight all occurrences
        String content = logTextArea.getText();
        int startIndex = 0;
        boolean foundAny = false;

        while ((startIndex = content.indexOf(searchTerm, startIndex)) != -1) {
            foundAny = true;
            logTextArea.selectRange(startIndex, startIndex + searchTerm.length());
            startIndex += searchTerm.length();
        }

        // If found, scroll to first match and select it
        if (foundAny) {
            startIndex = content.indexOf(searchTerm);
            logTextArea.selectRange(startIndex, startIndex + searchTerm.length());
            logTextArea.setStyle("-fx-control-inner-background: #fffacd; -fx-text-fill: #333333; -fx-font-family: 'Courier New'; -fx-font-size: 10; -fx-padding: 12; -fx-border-color: transparent;");

            // Scroll to first match
            int line = content.substring(0, startIndex).split("\n", -1).length - 1;
            logTextArea.positionCaret(startIndex);
        } else {
            // Reset styling if nothing found
            logTextArea.setStyle("-fx-control-inner-background: #ffffff; -fx-text-fill: #333333; -fx-font-family: 'Courier New'; -fx-font-size: 10; -fx-padding: 12; -fx-border-color: transparent;");
        }
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
                loadCurrentFile();
            } else {
                currentFilePath = null;
                logTextArea.clear();
                fileInfoLabel.setText("Ready");
                statusLabel.setText("No files open");
            }
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
        fileName.setStyle("-fx-font-size: 9; -fx-text-fill: #333333;");

        // Close button
        Button closeTab = new Button("✕");
        closeTab.setStyle(
            "-fx-padding: 0 4 0 4; " +
            "-fx-font-size: 8; " +
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #666666; " +
            "-fx-border-radius: 0; " +
            "-fx-min-width: 16; " +
            "-fx-min-height: 16; " +
            "-fx-padding: 2;"
        );

        closeTab.setOnAction(event -> closeFile(filePath));

        // On tab click, switch to this file
        tab.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && !event.getTarget().equals(closeTab)) {
                currentFilePath = filePath;
                tailThreadRef.setActive(false);
                pauseMode = false;
                pauseBtn.setText("⏸ Pause");
                pauseBtn.setStyle("-fx-padding: 5 10 5 10; -fx-font-size: 9; -fx-font-weight: bold; -fx-background-color: #FF9800; -fx-text-fill: white; -fx-border-radius: 0;");
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
