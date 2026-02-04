package org.taillogs.taillogs.utils;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FileOperations {

    public static long loadFileContent(TextArea textArea, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                showError("Error", "File not found: " + filePath);
                return 0;
            }

            StringBuilder content = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8)) {
                char[] buffer = new char[8192];
                int charsRead;
                while ((charsRead = reader.read(buffer)) != -1) {
                    content.append(buffer, 0, charsRead);
                }
            }

            Platform.runLater(() -> {
                textArea.clear();
                textArea.appendText(content.toString());
                textArea.setScrollTop(Double.MAX_VALUE);
            });

            return file.length();
        } catch (Exception e) {
            showError("Error", "Failed to read file: " + e.getMessage());
            return 0;
        }
    }

    public static void startTailing(String filePath, TextArea textArea, TailThreadRef threadRef) {
        if (!threadRef.isActive()) {
            threadRef.setActive(true);
            Thread tailThread = new Thread(() -> tailFile(filePath, textArea, threadRef), "TailThread");
            tailThread.setDaemon(true);
            tailThread.start();
        }
    }

    private static void tailFile(String filePath, TextArea textArea, TailThreadRef threadRef) {
        try {
            File file = new File(filePath);
            long filePosition = file.length();

            while (threadRef.isActive()) {
                try {
                    long currentSize = file.length();
                    if (currentSize > filePosition) {
                        try (InputStreamReader reader = new InputStreamReader(
                                new FileInputStream(file), StandardCharsets.UTF_8)) {
                            reader.skip(filePosition);
                            StringBuilder newContent = new StringBuilder();
                            char[] buffer = new char[8192];
                            int charsRead;
                            while ((charsRead = reader.read(buffer)) != -1) {
                                newContent.append(buffer, 0, charsRead);
                            }

                            if (newContent.length() > 0) {
                                String content = newContent.toString();
                                Platform.runLater(() -> {
                                    textArea.appendText(content);
                                    textArea.setScrollTop(Double.MAX_VALUE);
                                });
                                filePosition = currentSize;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore temporary read errors
                }

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void refreshFile(TextArea textArea, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                showError("Error", "File not found: " + filePath);
                return;
            }

            StringBuilder content = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8)) {
                char[] buffer = new char[8192];
                int charsRead;
                while ((charsRead = reader.read(buffer)) != -1) {
                    content.append(buffer, 0, charsRead);
                }
            }

            Platform.runLater(() -> {
                textArea.clear();
                textArea.appendText(content.toString());
                textArea.setScrollTop(Double.MAX_VALUE);
            });
        } catch (Exception e) {
            showError("Error", "Failed to refresh file: " + e.getMessage());
        }
    }

    public static List<String> getLogFiles(String folderPath) {
        try {
            Path path = Paths.get(folderPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return Collections.emptyList();
            }

            return Files.walk(path, 1)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.toLowerCase().endsWith(".log") ||
                                    name.toLowerCase().endsWith(".txt"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            showError("Error", "Failed to read folder: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<String> getLogFilesFullPath(String folderPath) {
        try {
            Path path = Paths.get(folderPath);
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return Collections.emptyList();
            }

            return Files.walk(path, 1)
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".log") || name.endsWith(".txt");
                    })
                    .map(Path::toString)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            showError("Error", "Failed to read folder: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static class TailThreadRef {
        private boolean active = false;

        public synchronized boolean isActive() {
            return active;
        }

        public synchronized void setActive(boolean active) {
            this.active = active;
        }
    }
}
