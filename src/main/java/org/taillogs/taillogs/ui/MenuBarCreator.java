package org.taillogs.taillogs.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert;
import javafx.application.Platform;

public class MenuBarCreator {
    private MenuBar menuBar;
    private MenuCallbacks callbacks;

    public interface MenuCallbacks {
        void onOpenFile();
        void onOpenFolder();
        void onExit();
        void onClearDisplay();
        void onRefreshFile();
        void onTogglePause();
        void onAbout();
        void onShortcuts();
        void onSetTheme(String theme);
    }

    public MenuBarCreator(MenuCallbacks callbacks) {
        this.callbacks = callbacks;
        createMenuBar();
    }

    private void createMenuBar() {
        menuBar = new MenuBar();
        // Set menu bar styling - bright blue background with white text
        menuBar.setStyle(
            "-fx-background-color: #2196F3; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-padding: 5px;"
        );

        // File Menu
        Menu fileMenu = new Menu("File");
        fileMenu.setStyle("-fx-text-fill: white;");

        MenuItem openFileItem = new MenuItem("📁 Open File");
        openFileItem.setOnAction(e -> callbacks.onOpenFile());

        MenuItem openFolderItem = new MenuItem("📂 Open Folder");
        openFolderItem.setOnAction(e -> callbacks.onOpenFolder());

        MenuItem recentFilesItem = new MenuItem("Recent Files");
        recentFilesItem.setOnAction(e -> showInfo("Recent Files", "No recent files yet"));
        recentFilesItem.setDisable(true);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> callbacks.onExit());

        fileMenu.getItems().addAll(openFileItem, openFolderItem, new javafx.scene.control.SeparatorMenuItem(),
                recentFilesItem, new javafx.scene.control.SeparatorMenuItem(), exitItem);

        // Tools Menu
        Menu toolsMenu = new Menu("Tools");
        toolsMenu.setStyle("-fx-text-fill: white;");

        MenuItem clearItem = new MenuItem("📋 Clear Display");
        clearItem.setOnAction(e -> callbacks.onClearDisplay());

        MenuItem refreshItem = new MenuItem("🔄 Refresh File");
        refreshItem.setOnAction(e -> callbacks.onRefreshFile());

        MenuItem pauseItem = new MenuItem("⏸ Pause/Resume");
        pauseItem.setOnAction(e -> callbacks.onTogglePause());

        MenuItem findReplaceItem = new MenuItem("Find & Replace");
        findReplaceItem.setOnAction(e -> showInfo("Find & Replace", "Feature coming soon"));
        findReplaceItem.setDisable(true);

        toolsMenu.getItems().addAll(clearItem, refreshItem, pauseItem,
                new javafx.scene.control.SeparatorMenuItem(), findReplaceItem);

        // Appearance Menu
        Menu appearanceMenu = new Menu("Appearance");
        appearanceMenu.setStyle("-fx-text-fill: white;");

        MenuItem lightThemeItem = new MenuItem("☀ Light Theme");
        lightThemeItem.setOnAction(e -> callbacks.onSetTheme("light"));

        MenuItem darkThemeItem = new MenuItem("🌙 Dark Theme");
        darkThemeItem.setOnAction(e -> callbacks.onSetTheme("dark"));

        MenuItem monokaiThemeItem = new MenuItem("🎨 Monokai Theme");
        monokaiThemeItem.setOnAction(e -> callbacks.onSetTheme("monokai"));

        appearanceMenu.getItems().addAll(lightThemeItem, darkThemeItem, monokaiThemeItem);

        // Help Menu
        Menu helpMenu = new Menu("Help");
        helpMenu.setStyle("-fx-text-fill: white;");

        MenuItem aboutItem = new MenuItem("ℹ About Tail Logs");
        aboutItem.setOnAction(e -> callbacks.onAbout());

        MenuItem shortcutsItem = new MenuItem("⌨ Keyboard Shortcuts");
        shortcutsItem.setOnAction(e -> callbacks.onShortcuts());

        MenuItem docsItem = new MenuItem("📖 Documentation");
        docsItem.setOnAction(e -> showInfo("Documentation", "Visit docs.example.com for full documentation"));
        docsItem.setDisable(true);

        helpMenu.getItems().addAll(aboutItem, shortcutsItem, new javafx.scene.control.SeparatorMenuItem(), docsItem);

        // Add menus to menu bar
        menuBar.getMenus().addAll(fileMenu, toolsMenu, appearanceMenu, helpMenu);
    }

    public MenuBar getMenuBar() {
        return menuBar;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
