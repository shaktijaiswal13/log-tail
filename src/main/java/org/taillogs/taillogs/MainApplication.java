package org.taillogs.taillogs;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;

import org.taillogs.taillogs.screens.HomeController;
import org.taillogs.taillogs.screens.ApplicationController;
import org.taillogs.taillogs.screens.SettingsController;
import org.taillogs.taillogs.ui.MenuBarCreator;
import org.taillogs.taillogs.config.AppearanceSettings;
import org.taillogs.taillogs.config.PreferencesManager;

import javafx.scene.image.Image;

import java.awt.Taskbar;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class MainApplication extends Application {
    private Stage primaryStage;
    private Scene homeScene;
    private Scene appScene;
    private HomeController homeController;
    private ApplicationController appController;
    private MenuBarCreator menuBarCreator;

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        // Initialize scenes
        initializeHomeScene();
        initializeAppScene();

        // Set up window properties
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("app-icon.png")));
        // Set macOS dock icon
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                taskbar.setIconImage(ImageIO.read(getClass().getResourceAsStream("app-icon.png")));
            }
        } catch (Exception e) {
            // Ignore - dock icon is non-critical
        }
        primaryStage.setTitle("Tail Logs");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(700);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);

        // Show home scene
        showHomeScene();

        primaryStage.show();
    }

    private void initializeHomeScene() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("home-view.fxml"));
        homeScene = new Scene(fxmlLoader.load());
        homeController = fxmlLoader.getController();

        // Set up callbacks
        homeController.setOnFileSelected(() -> {
            String filePath = homeController.getSelectedFilePath();
            if (filePath != null) {
                appController.setCurrentFile(filePath);
                showAppScene();
                homeController.clearSelection();
            }
        });

        homeController.setOnFolderSelected(() -> {
            String folderPath = homeController.getSelectedFolderPath();
            if (folderPath != null) {
                appController.setCurrentFolder(folderPath);
                showAppScene();
                homeController.clearSelection();
            }
        });

        homeController.setOnEnterApplication(() -> {
            showAppScene();
        });
    }

    private void initializeAppScene() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("app-view.fxml"));
        appScene = new Scene(fxmlLoader.load());
        appController = fxmlLoader.getController();

        appController.setOnBack(this::showHomeScene);

        // Create and set up menu bar with callbacks
        menuBarCreator = new MenuBarCreator(new MenuBarCreator.MenuCallbacks() {
            @Override
            public void onOpenFile() {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select a file");

                FileChooser.ExtensionFilter textFilter = new FileChooser.ExtensionFilter(
                        "Text and log files",
                        "*.log", "*.txt", "*.out", "*.json", "*.yaml", "*.yml", "*.xml", "*.csv", "*.conf");
                FileChooser.ExtensionFilter allFilesFilter = new FileChooser.ExtensionFilter("All files (*.*)", "*.*");

                fileChooser.getExtensionFilters().addAll(textFilter, allFilesFilter);
                fileChooser.setSelectedExtensionFilter(textFilter);

                File selectedFile = fileChooser.showOpenDialog(primaryStage);
                if (selectedFile != null) {
                    appController.setCurrentFile(selectedFile.getAbsolutePath());
                    showAppScene();
                }
            }

            @Override
            public void onOpenFolder() {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Select a folder containing log files");
                File selectedFolder = directoryChooser.showDialog(primaryStage);
                if (selectedFolder != null) {
                    appController.setCurrentFolder(selectedFolder.getAbsolutePath());
                    showAppScene();
                }
            }

            @Override
            public void onOpenRecentFile(String filePath) {
                File file = new File(filePath);
                if (file.exists()) {
                    appController.setCurrentFile(filePath);
                    menuBarCreator.refreshRecentFilesMenu();
                    showAppScene();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("File Not Found");
                    alert.setHeaderText("Cannot Open File");
                    alert.setContentText("File no longer exists: " + filePath);
                    alert.showAndWait();
                    PreferencesManager.removeRecentFile(filePath);
                    menuBarCreator.refreshRecentFilesMenu();
                }
            }

            @Override
            public void onExit() {
                appController.stopTailing();
                primaryStage.close();
            }

            @Override
            public void onClearDisplay() {
                appController.clearDisplay();
            }

            @Override
            public void onRefreshFile() {
                appController.refreshDisplay();
            }

            @Override
            public void onTogglePause() {
                appController.togglePause();
            }

            @Override
            public void onAbout() {
                showAboutDialog();
            }

            @Override
            public void onShortcuts() {
                showShortcutsDialog();
            }

            @Override
            public void onSetTheme(String theme) {
                showThemeDialog(theme);
            }

            @Override
            public void onSettings() {
                showSettingsDialog();
            }
        });

        // Add menu bar to the app scene
        appController.setMenuBar(menuBarCreator.getMenuBar());
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Tail Logs");
        alert.setHeaderText("Tail Logs v1.0");
        alert.setContentText(
            "A powerful log file viewer and monitor for macOS-style log tailing.\n\n" +
            "Features:\n" +
            "• Real-time log file monitoring\n" +
            "• Advanced search and filtering\n" +
            "• Multiple file browsing\n" +
            "• Pause/resume functionality\n\n" +
            "© 2026 Tail Logs"
        );
        alert.showAndWait();
    }

    private void showShortcutsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Available Shortcuts");
        alert.setContentText(
            "Common Shortcuts:\n\n" +
            "Ctrl/Cmd+O - Open file\n" +
            "Ctrl/Cmd+F - Open folder\n" +
            "Ctrl/Cmd+Q - Quit application\n" +
            "Ctrl/Cmd+L - Clear display\n" +
            "Ctrl/Cmd+R - Refresh file\n\n" +
            "Note: These shortcuts can be customized in Settings."
        );
        alert.showAndWait();
    }

    private void showThemeDialog(String theme) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Theme Changed");
        alert.setHeaderText("Theme Changed");
        alert.setContentText("Theme changed to " + theme.substring(0, 1).toUpperCase() + theme.substring(1));
        alert.showAndWait();
    }

    private void showSettingsDialog() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("settings-view.fxml"));
            javafx.scene.layout.VBox root = fxmlLoader.load();
            SettingsController settingsController = fxmlLoader.getController();

            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.setScene(new Scene(root, 500, 300));
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(primaryStage);

            settingsStage.showAndWait();

            if (settingsController.isOkPressed()) {
                AppearanceSettings settings = settingsController.getSettings();
                PreferencesManager.saveAppearanceSettings(settings);

                // Apply settings to current view
                appController.applyAppearanceSettings(settings);
                menuBarCreator.applyAppearanceSettings(settings);
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open settings");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void showHomeScene() {
        primaryStage.setScene(homeScene);
    }

    public void showAppScene() {
        primaryStage.setScene(appScene);
    }

    public static void main(String[] args) {
        launch();
    }
}
