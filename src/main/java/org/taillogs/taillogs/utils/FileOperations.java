package org.taillogs.taillogs.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileOperations {

    public static long loadFileContent(CodeArea textArea, String filePath) {
        return loadFileContent(textArea, filePath, null);
    }

    public static long loadFileContent(CodeArea textArea, String filePath, Runnable highlightCallback) {
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
                // Apply highlighting
                if (highlightCallback != null) {
                    highlightCallback.run();
                } else {
                    SyntaxHighlighter.applyLogLevelHighlighting(textArea);
                }
                // Scroll to end
                textArea.moveTo(textArea.getLength());
                textArea.requestFollowCaret();
            });

            return file.length();
        } catch (Exception e) {
            showError("Error", "Failed to read file: " + e.getMessage());
            return 0;
        }
    }

    public static void startTailing(String filePath, CodeArea textArea, TailThreadRef threadRef) {
        startTailing(filePath, textArea, threadRef, null);
    }

    public static void startTailing(String filePath, CodeArea textArea, TailThreadRef threadRef, Runnable highlightCallback) {
        synchronized (threadRef) {
            threadRef.setActive(true);
            Thread existing = threadRef.getTailThread();
            if (existing != null && existing.isAlive()) {
                return;
            }

            Thread tailThread = new Thread(() -> tailFile(filePath, textArea, threadRef, highlightCallback), "TailThread");
            tailThread.setDaemon(true);
            threadRef.setTailThread(tailThread);
            tailThread.start();
        }
    }

    private static void tailFile(String filePath, CodeArea textArea, TailThreadRef threadRef, Runnable highlightCallback) {
        try {
            File file = new File(filePath);
            // Initialize filePosition if this is the first time tailing
            if (threadRef.getFilePosition() == 0) {
                threadRef.setFilePosition(file.length());
            }
            long lastKnownModified = file.lastModified();

            while (threadRef.isActive()) {
                try {
                    long currentSize = file.length();
                    long filePosition = threadRef.getFilePosition();
                    long currentModified = file.lastModified();

                    // Handle truncate/replace cases where the file shrinks or is rewritten in-place.
                    if (currentSize < filePosition || (currentModified > lastKnownModified && currentSize == filePosition)) {
                        filePosition = 0;
                        threadRef.setFilePosition(0);
                    }

                    if (currentSize > filePosition) {
                        try (RandomAccessFile reader = new RandomAccessFile(file, "r");
                             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                            reader.seek(filePosition);
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = reader.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }

                            StringBuilder newContent = new StringBuilder();
                            String decoded = out.toString(StandardCharsets.UTF_8);
                            if (!decoded.isEmpty()) {
                                newContent.append(decoded);
                            }

                            if (newContent.length() > 0) {
                                String content = newContent.toString();
                                Platform.runLater(() -> {
                                    textArea.appendText(content);
                                    // Apply highlighting
                                    if (highlightCallback != null) {
                                        highlightCallback.run();
                                    } else {
                                        SyntaxHighlighter.applyLogLevelHighlighting(textArea);
                                    }
                                    // Scroll to end
                                    textArea.moveTo(textArea.getLength());
                                    textArea.requestFollowCaret();
                                });
                                threadRef.setFilePosition(currentSize);
                            }
                        }
                    }
                    lastKnownModified = currentModified;
                } catch (Exception e) {
                    // Ignore temporary read errors
                }

                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            threadRef.setTailThread(null);
        }
    }

    public static void refreshFile(CodeArea textArea, String filePath) {
        refreshFile(textArea, filePath, null);
    }

    public static void refreshFile(CodeArea textArea, String filePath, Runnable highlightCallback) {
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
                // Apply highlighting
                if (highlightCallback != null) {
                    highlightCallback.run();
                } else {
                    SyntaxHighlighter.applyLogLevelHighlighting(textArea);
                }
                // Scroll to end
                textArea.moveTo(textArea.getLength());
                textArea.requestFollowCaret();
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
        private long filePosition = 0;
        private Thread tailThread;

        public synchronized boolean isActive() {
            return active;
        }

        public synchronized void setActive(boolean active) {
            this.active = active;
            if (!active && tailThread != null) {
                tailThread.interrupt();
            }
        }

        public synchronized long getFilePosition() {
            return filePosition;
        }

        public synchronized void setFilePosition(long position) {
            this.filePosition = position;
        }

        public synchronized Thread getTailThread() {
            return tailThread;
        }

        public synchronized void setTailThread(Thread tailThread) {
            this.tailThread = tailThread;
        }
    }
}
