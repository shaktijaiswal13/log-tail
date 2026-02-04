package org.taillogs.taillogs;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;

import org.taillogs.taillogs.screens.HomeController;
import org.taillogs.taillogs.screens.ApplicationController;
import org.taillogs.taillogs.ui.MenuBarCreator;

import java.io.File;
import java.io.IOException;

public class HelloApplication extends Application {
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
                fileChooser.setTitle("Select a log file");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Log files (*.log)", "*.log"),
                        new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"),
                        new FileChooser.ExtensionFilter("All files (*.*)", "*.*")
                );
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
            "Ctrl+O - Open file\n" +
            "Ctrl+F - Open folder\n" +
            "Ctrl+Q - Quit application\n" +
            "Ctrl+L - Clear display\n" +
            "Ctrl+R - Refresh file\n\n" +
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
