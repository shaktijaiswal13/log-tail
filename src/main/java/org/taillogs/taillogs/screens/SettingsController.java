package org.taillogs.taillogs.screens;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;
import org.taillogs.taillogs.config.AppearanceSettings;
import org.taillogs.taillogs.config.PreferencesManager;

public class SettingsController {
    @FXML
    private Spinner<Integer> fontSizeSpinner;

    @FXML
    private ComboBox<String> fontWeightCombo;

    @FXML
    private Label fontSizePreview;

    @FXML
    private Label fontWeightPreview;

    @FXML
    private Button okBtn;

    @FXML
    private Button cancelBtn;

    private AppearanceSettings currentSettings;
    private boolean okPressed = false;

    @FXML
    public void initialize() {
        // Load current settings
        currentSettings = PreferencesManager.loadAppearanceSettings();

        // Initialize font size spinner
        fontSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 24, currentSettings.getFontSize()));
        fontSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateFontSizePreview(newVal);
        });

        // Initialize font weight combo
        fontWeightCombo.getItems().addAll("Regular", "Bold");
        fontWeightCombo.setValue(currentSettings.getFontWeight());
        fontWeightCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateFontWeightPreview(newVal);
        });

        // Update previews with current values
        updateFontSizePreview(currentSettings.getFontSize());
        updateFontWeightPreview(currentSettings.getFontWeight());

        // Setup button handlers
        okBtn.setOnAction(event -> onOk());
        cancelBtn.setOnAction(event -> onCancel());
    }

    private void updateFontSizePreview(int size) {
        fontSizePreview.setStyle("-fx-font-size: " + size + "; -fx-font-family: 'Courier New';");
    }

    private void updateFontWeightPreview(String weight) {
        String style = "-fx-font-size: 13; -fx-font-family: 'Courier New';";
        if (weight.equals("Bold")) {
            style += " -fx-font-weight: bold;";
        }
        fontWeightPreview.setStyle(style);
    }

    @FXML
    private void onOk() {
        okPressed = true;
        closeDialog();
    }

    @FXML
    private void onCancel() {
        okPressed = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) okBtn.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    public AppearanceSettings getSettings() {
        return new AppearanceSettings(
                fontSizeSpinner.getValue(),
                fontWeightCombo.getValue()
        );
    }

    public boolean isOkPressed() {
        return okPressed;
    }
}
