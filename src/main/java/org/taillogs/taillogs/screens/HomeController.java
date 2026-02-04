package org.taillogs.taillogs.screens;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class HomeController {
    @FXML
    private Button openFileBtn;
    @FXML
    private Button openFolderBtn;
    @FXML
    private Button enterAppBtn;

    private Runnable onFileSelected;
    private Runnable onFolderSelected;
    private Runnable onEnterApplication;
    private String selectedFilePath;
    private String selectedFolderPath;

    public void initialize() {
        // Controller initialization
    }

    @FXML
    protected void onOpenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a log file");

        FileChooser.ExtensionFilter logFilter = new FileChooser.ExtensionFilter("Log files (*.log)", "*.log");
        FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt");
        FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All files (*.*)", "*.*");

        fileChooser.getExtensionFilters().addAll(logFilter, txtFilter, allFilter);

        Stage stage = (Stage) openFileBtn.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            selectedFilePath = selectedFile.getAbsolutePath();
            if (onFileSelected != null) {
                onFileSelected.run();
            }
        }
    }

    @FXML
    protected void onOpenFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a folder containing log files");

        Stage stage = (Stage) openFolderBtn.getScene().getWindow();
        File selectedFolder = directoryChooser.showDialog(stage);

        if (selectedFolder != null) {
            selectedFolderPath = selectedFolder.getAbsolutePath();
            if (onFolderSelected != null) {
                onFolderSelected.run();
            }
        }
    }

    @FXML
    protected void onEnterApplication() {
        if (onEnterApplication != null) {
            onEnterApplication.run();
        }
    }

    // Setters for callbacks
    public void setOnFileSelected(Runnable callback) {
        this.onFileSelected = callback;
    }

    public void setOnFolderSelected(Runnable callback) {
        this.onFolderSelected = callback;
    }

    public void setOnEnterApplication(Runnable callback) {
        this.onEnterApplication = callback;
    }

    // Getters for selected paths
    public String getSelectedFilePath() {
        return selectedFilePath;
    }

    public String getSelectedFolderPath() {
        return selectedFolderPath;
    }

    public void clearSelection() {
        selectedFilePath = null;
        selectedFolderPath = null;
    }
}
