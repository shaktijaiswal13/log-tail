package org.taillogs.taillogs.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert;
import javafx.application.Platform;
import org.taillogs.taillogs.config.AppearanceSettings;
import org.taillogs.taillogs.config.PreferencesManager;
import org.taillogs.taillogs.models.RecentFile;
import org.taillogs.taillogs.utils.FontStylesUtil;
import java.util.List;

public class MenuBarCreator {
    private MenuBar menuBar;
    private MenuCallbacks callbacks;
    private Menu recentFilesMenu;

    public interface MenuCallbacks {
        void onOpenFile();
        void onOpenFolder();
        void onOpenRecentFile(String filePath);
        void onExit();
        void onClearDisplay();
        void onRefreshFile();
        void onTogglePause();
        void onAbout();
        void onShortcuts();
        void onSetTheme(String theme);
        void onSettings();
    }

    public MenuBarCreator(MenuCallbacks callbacks) {
        this.callbacks = callbacks;
        createMenuBar();
    }

    private void createMenuBar() {
        menuBar = new MenuBar();
        // Set menu bar styling - bright blue background with white text
        AppearanceSettings defaultSettings = new AppearanceSettings();
        menuBar.setStyle(FontStylesUtil.getMenuBarStyle(defaultSettings));

        // File Menu
        Menu fileMenu = new Menu("File");
        fileMenu.setStyle("-fx-text-fill: white;");

        MenuItem openFileItem = new MenuItem("📁 Open File");
        openFileItem.setOnAction(e -> callbacks.onOpenFile());

        MenuItem openFolderItem = new MenuItem("📂 Open Folder");
        openFolderItem.setOnAction(e -> callbacks.onOpenFolder());

        recentFilesMenu = new Menu("Recent Files");
        updateRecentFilesMenu(recentFilesMenu);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> callbacks.onExit());

        fileMenu.getItems().addAll(openFileItem, openFolderItem, new javafx.scene.control.SeparatorMenuItem(),
                recentFilesMenu, new javafx.scene.control.SeparatorMenuItem(), exitItem);

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

        MenuItem settingsItem = new MenuItem("⚙ Settings");
        settingsItem.setOnAction(e -> callbacks.onSettings());

        MenuItem lightThemeItem = new MenuItem("☀ Light Theme");
        lightThemeItem.setOnAction(e -> callbacks.onSetTheme("light"));

        MenuItem darkThemeItem = new MenuItem("🌙 Dark Theme");
        darkThemeItem.setOnAction(e -> callbacks.onSetTheme("dark"));

        MenuItem monokaiThemeItem = new MenuItem("🎨 Monokai Theme");
        monokaiThemeItem.setOnAction(e -> callbacks.onSetTheme("monokai"));

        appearanceMenu.getItems().addAll(settingsItem, new javafx.scene.control.SeparatorMenuItem(),
                lightThemeItem, darkThemeItem, monokaiThemeItem);

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

    public void applyAppearanceSettings(AppearanceSettings settings) {
        menuBar.setStyle(FontStylesUtil.getMenuBarStyle(settings));
    }

    public void refreshRecentFilesMenu() {
        if (recentFilesMenu != null) {
            updateRecentFilesMenu(recentFilesMenu);
        }
    }

    private void updateRecentFilesMenu(Menu menu) {
        menu.getItems().clear();
        List<RecentFile> recent = PreferencesManager.loadRecentFiles();

        if (recent.isEmpty()) {
            MenuItem noFiles = new MenuItem("No recent files");
            noFiles.setDisable(true);
            menu.getItems().add(noFiles);
        } else {
            for (RecentFile rf : recent) {
                MenuItem item = new MenuItem(rf.getFileName());
                item.setOnAction(e -> callbacks.onOpenRecentFile(rf.getFilePath()));
                menu.getItems().add(item);
            }
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
