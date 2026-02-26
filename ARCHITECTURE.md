# Tail Logs - Architecture & Design Guide

## Table of Contents
1. [High-Level Architecture](#high-level-architecture)
2. [Design Patterns](#design-patterns)
3. [Component Details](#component-details)
4. [Data Flow Patterns](#data-flow-patterns)
5. [Extension Points](#extension-points)
6. [Threading Model](#threading-model)

---

## High-Level Architecture

### System Layers

```
┌─────────────────────────────────────────┐
│         PRESENTATION LAYER              │
│  (UI Controllers + FXML + CSS)          │
├─────────────────────────────────────────┤
│       APPLICATION LAYER                 │
│  (RightPanelController, MenuBarCreator) │
├─────────────────────────────────────────┤
│      BUSINESS LOGIC LAYER               │
│  (Managers: Highlight, Filter, Bookmark)│
├─────────────────────────────────────────┤
│         DATA ACCESS LAYER               │
│  (PreferencesManager, FileOperations)   │
├─────────────────────────────────────────┤
│         PERSISTENCE LAYER               │
│  (JSON files at ~/.tail_logs/)          │
└─────────────────────────────────────────┘
```

### Component Organization

```
APPLICATION
    ├── Scenes
    │   ├── HomeScene (HomeController)
    │   └── ApplicationScene (ApplicationController)
    │
    ├── UI Components
    │   ├── MenuBar (MenuBarCreator)
    │   ├── CodeArea (RichTextFX)
    │   ├── RightPanel (RightPanelController)
    │   └── StatusBar
    │
    ├── Business Logic
    │   ├── HighlightManager
    │   ├── FilterManager
    │   └── BookmarkManager
    │
    ├── Data Models
    │   ├── HighlightPattern
    │   ├── FilterRule
    │   ├── Bookmark
    │   └── RecentFile
    │
    ├── Configuration
    │   ├── PreferencesManager
    │   ├── AppearanceSettings
    │   └── AppConfig
    │
    └── Utilities
        ├── FileOperations
        ├── SyntaxHighlighter
        └── FontStylesUtil
```

---

## Design Patterns

### 1. Model-View-Controller (MVC)

**Where Used:** UI Layout and User Interaction

```
Model ← → View ← → Controller
(FXML)   (JavaFX) (Java Code)
```

**Implementation:**
- **Models:** `HighlightPattern`, `FilterRule`, `Bookmark` classes
- **Views:** FXML files (app-view.fxml, right-panel-view.fxml, etc.)
- **Controllers:** `ApplicationController`, `RightPanelController`, etc.

**Example Flow:**
```java
// User adds highlight pattern
RightPanelController.addPatternButtonClicked()
    ↓
HighlightPattern model created
    ↓
HighlightManager.addHighlightPattern(pattern)
    ↓
ObservableList updated (notifies View)
    ↓
ListView refreshed (highlights tab updated)
```

### 2. Manager Pattern

**Where Used:** Business Logic Separation

Each manager handles one domain and maintains:
- Internal state (ObservableList)
- Persistence logic (load/save)
- Business rules (AND logic, priority system)

```java
public class HighlightManager {
    private final ObservableList<HighlightPattern> patterns;
    private String currentFilePath;

    public void addHighlightPattern(...) { /* ... */ }
    public void removeHighlightPattern(...) { /* ... */ }
    public void setCurrentFile(File) { /* ... */ }
    public ObservableList<HighlightPattern> getPatterns() { /* ... */ }
}
```

**Advantages:**
- Single Responsibility Principle
- Easy to test independently
- Reusable across controllers
- Clear separation of concerns

### 3. Configuration/Singleton Pattern

**Where Used:** Centralized Persistence

```
PreferencesManager (Singleton)
    ├── Load/Save Appearance
    ├── Load/Save Highlights
    ├── Load/Save Filters
    ├── Load/Save Bookmarks
    └── Load/Save Recent Files
```

Only one instance manages all file I/O, preventing race conditions.

### 4. Observer Pattern

**Where Used:** Reactive UI Updates

```
Data Model (ObservableList)
        ↓ (notifies)
     Observers
        ↓ (callback)
      UI Updates
```

**Example:**
```java
// In RightPanelController
highlightManager.getPatterns().addListener((ListChangeListener<HighlightPattern>) change -> {
    // UI list updates automatically when patterns change
});
```

### 5. Callback/Listener Pattern

**Where Used:** Loose Coupling

Components communicate through interfaces instead of direct references:

```
ApplicationController
    ├── onFileSelected() → calls FileOperations.loadFile()
    ├── onSearchChanged() → calls HighlightManager.applyCombinedHighlighting()
    └── onHighlightAdded() → reapplies highlighting
```

**Advantage:** Low coupling, components can be tested independently

### 6. Strategy Pattern

**Where Used:** Filtering & Highlighting Algorithms

Different strategies for applying effects:
- **Search highlighting** (highest priority, yellow)
- **Custom pattern highlighting** (medium priority, custom colors)
- **Log-level highlighting** (lowest priority, ERROR/WARN/INFO)

```java
public void applyCombinedHighlighting() {
    // Apply in order of priority
    applyLogLevelHighlighting();      // Base layer
    applyCustomPatternHighlighting(); // Middle layer
    applySearchHighlighting();        // Top layer
}
```

---

## Component Details

### ApplicationController (947 lines) - CORE

**Responsibilities:**
- Manages main application scene
- Handles file opening/switching
- Coordinates all managers
- Implements search and filter logic
- Manages tailing lifecycle

**Key Properties:**
```java
private CodeArea codeArea;                    // RichTextFX display
private ObservableList<String> openFiles;    // Currently open files
private String currentFilePath;               // Active file
private Map<String, String> fileContentCache;// File content storage
private HighlightManager highlightManager;
private FilterManager filterManager;
private BookmarkManager bookmarkManager;
```

**Key Methods:**
```java
setCurrentFile(File file)          // Switch active file
openFile(File file)                // Add new file to open list
closeFile(String filePath)         // Remove file from open list
loadFile(File file)                // Load content into display
searchContent()                    // Apply search highlighting
filterContent()                    // Apply filter rules
applyHighlighting()                // Apply all highlighting styles
```

### HighlightManager (399 lines)

**Responsibilities:**
- Manage custom highlight patterns
- Detect log levels (ERROR, WARN, INFO)
- Merge multiple highlighting sources
- Generate versioned CSS stylesheets
- Per-file and global storage

**Algorithm - Combined Highlighting Priority:**
```
Search Result (Yellow, Highest Priority)
    ↓ overlays
Custom Patterns (User-defined colors)
    ↓ overlays
Log-Level Detection (ERROR/WARN/INFO)
    ↓ overlays
Default Black Text (Base)
```

**Storage Strategy:**
```
~/.tail_logs/
├── highlights.json               # Global patterns (fallback)
└── highlights_<fileHash>.json    # Per-file patterns (preferred)
```

**Per-File Logic:**
1. When file opened: `setCurrentFile(file)` called
2. Manager loads `highlights_<fileHash>.json` first
3. If not found, loads from `highlights.json` (global)
4. If neither exists, starts with empty list
5. When pattern added/removed: saves to per-file storage

### FilterManager (169 lines)

**Responsibilities:**
- Store and manage filter rules
- Apply AND logic across rules
- Preserve original line numbers
- Per-file and global storage

**Filtering Algorithm:**
```
For each line in content:
  FOR EACH enabled filter rule:
    IF line matches pattern:
      Keep line
    ELSE:
      Exclude line (fails filter)

Result: Only lines matching ALL rules shown
Format: [originalLineNum] content
```

**Storage Strategy:**
Same as HighlightManager:
- Global: `filters.json`
- Per-file: `filters_<fileHash>.json`

### BookmarkManager (74 lines)

**Responsibilities:**
- Store bookmarks for current file
- Per-file storage only (no global fallback)
- Provide observable list for UI binding

**Storage Strategy:**
- Per-file only: `bookmarks_<fileHash>.json`
- No global fallback (each file has separate bookmarks)
- Cleared when switching files

### PreferencesManager (277 lines)

**Responsibilities:**
- Centralized configuration management
- Load/save all application settings
- Directory creation and management
- JSON serialization/deserialization

**Directory Structure:**
```
~/.tail_logs/
├── All configuration files
└── All dynamic CSS files
```

**Methods:**
```java
// Appearance
saveAppearanceSettings(AppearanceSettings)
loadAppearanceSettings() → AppearanceSettings

// Highlights
saveHighlightPatterns(filePath, patterns)
loadHighlightPatterns(filePath) → List<HighlightPattern>
getGlobalHighlightPatterns() → List<HighlightPattern>

// Filters
saveFilterRules(filePath, rules)
loadFilterRules(filePath) → List<FilterRule>
getGlobalFilterRules() → List<FilterRule>

// Bookmarks (per-file only)
saveBookmarks(filePath, bookmarks)
loadBookmarks(filePath) → List<Bookmark>

// Recent Files
addRecentFile(filePath)
loadRecentFiles() → List<RecentFile>
removeRecentFile(filePath)

// Utilities
encodeFileKey(filePath) → String hash
ensureConfigDirectory()
```

---

## Data Flow Patterns

### 1. File Opening Workflow

```
User clicks "File → Open File"
    ↓
FileChooser dialog shown
    ↓
User selects file
    ↓
ApplicationController.setCurrentFile(file)
    ↓
    ├→ highlightManager.setCurrentFile(file)
    │   ├→ Load per-file highlights_<hash>.json
    │   └→ If missing, load global highlights.json
    │
    ├→ filterManager.setCurrentFile(file)
    │   ├→ Load per-file filters_<hash>.json
    │   └→ If missing, load global filters.json
    │
    ├→ bookmarkManager.setCurrentFile(file)
    │   └→ Load per-file bookmarks_<hash>.json
    │
    ├→ FileOperations.loadFileContent(file)
    │   ├→ Read file into memory
    │   └→ Cache content
    │
    └→ applyHighlighting() + filterContent()
        └→ Display in CodeArea with styles

    ↓
    PreferencesManager.addRecentFile(file)
        └→ Update recent_files.json (max 10)

    ↓
    MenuBarCreator.updateRecentFilesMenu()
        └→ Refresh recent files menu
```

### 2. Real-Time Tailing Workflow

```
ApplicationController.loadFile() calls FileOperations.startTailing(file)
    ↓
FileOperations spawns background thread (daemon)
    ↓
Background thread loop:
  while tailing enabled:
    sleep(1 second)  // Check interval
    Get current file size

    IF size increased:
      Read new content from last position
      Platform.runLater() → UI thread safe

    INVOKE callback (ApplicationController.onNewContent)
        ↓
        Apply filters → FilterManager.filterContent()
        Apply highlighting → HighlightManager.applyCombinedHighlighting()
        Update CodeArea with new content + styles
        Update line count in status bar
```

### 3. Search Highlighting Workflow

```
User types in search box
    ↓
ApplicationController.searchContent() triggered
    ↓
    ├→ If search empty: clear search highlighting
    │   └→ Reapply only custom + log-level highlighting
    │
    └→ If search has text:
        ├→ Apply filters (FilterManager.filterContent())
        ├→ Find all search matches (case-insensitive)
        ├→ Build StyleSpans with yellow background
        ├→ Apply custom pattern highlights (underneath)
        ├→ Apply log-level highlights (underneath)
        └→ Apply combined to CodeArea

CodeArea updates with search highlights on top of others
```

### 4. Configuration Persistence Workflow

```
USER ACTION: Add highlight pattern in right panel
    ↓
RightPanelController.onAddPatternClicked()
    ↓
HighlightManager.addHighlightPattern(pattern, color, isRegex)
    ├→ Create new HighlightPattern object
    ├→ Add to patterns ObservableList
    └→ Notify listeners (UI updates automatically)
    ↓
ApplicationController observes change
    ↓
    ├→ Reapply highlighting to CodeArea
    └→ Schedule persistence save

    ↓
ON APP EXIT or ON FILE CHANGE:
    ↓
    PreferencesManager.saveHighlightPatterns(currentFile, patterns)
        ├→ Get file path hash
        ├→ Serialize patterns to JSON via Gson
        ├→ Write to ~/.tail_logs/highlights_<hash>.json
        └→ Return (gracefully handle errors)

NEXT SESSION - File reopened:
    ↓
    HighlightManager.setCurrentFile(file)
        ├→ PreferencesManager.loadHighlightPatterns(file)
        ├→ Check ~/.tail_logs/highlights_<hash>.json
        ├→ Parse JSON via Gson
        ├→ Return patterns (or empty list if missing)
        └→ Populate patterns ObservableList

    ↓
RightPanelController observes change
    ↓
    ListViews in right panel update automatically
```

---

## Extension Points

### Adding a New Manager (e.g., SnippetManager)

1. **Create Manager Class:**
```java
public class SnippetManager {
    private final ObservableList<Snippet> snippets = FXCollections.observableArrayList();
    private String currentFilePath;

    public void setCurrentFile(String filePath) {
        this.currentFilePath = filePath;
        loadSnippets();
    }

    public void addSnippet(Snippet snippet) {
        snippets.add(snippet);
        saveSnippets();
    }

    private void loadSnippets() {
        // Load from PreferencesManager
    }

    private void saveSnippets() {
        // Save via PreferencesManager
    }
}
```

2. **Create UI Tab:**
   - Add FXML elements to right-panel-view.fxml
   - Add tab in RightPanelController

3. **Create Data Model:**
```java
public class Snippet {
    private String uuid;
    private String name;
    private String content;
    // getters/setters
}
```

4. **Update PreferencesManager:**
```java
public void saveSnippets(String filePath, List<Snippet> snippets) {
    // Implementation
}

public List<Snippet> loadSnippets(String filePath) {
    // Implementation
}
```

5. **Wire in ApplicationController:**
```java
private SnippetManager snippetManager;

// In initialization
snippetManager = new SnippetManager();
snippetManager.getSnippets().addListener((ListChangeListener<Snippet>) change -> {
    // UI update callback
});
```

### Adding New Appearance Settings

1. **Extend AppearanceSettings:**
```java
public class AppearanceSettings {
    // Existing...
    private String accentColor;  // New
    private boolean darkMode;     // New
}
```

2. **Update SettingsController FXML & logic**

3. **Update PreferencesManager save/load methods**

4. **Apply in MainApplication or ApplicationController**

---

## Threading Model

### Main Thread (JavaFX Thread)
- Handles all UI updates
- Processes user interactions
- Updates CodeArea display

### File Tailing Thread (Background)
- Monitors file for changes
- Reads new content
- **Invokes UI updates via `Platform.runLater()`**
- One thread per tailed file
- Daemon thread (auto-stops on app exit)

### Code Safety
```java
// In FileOperations - tailing thread
new Thread(() -> {
    while (tailing && !Thread.currentThread().isInterrupted()) {
        // ... check file size ...

        // SAFE: Invoke callback on UI thread
        Platform.runLater(() -> {
            callback.onNewContent(newLines);
        });
    }
}).setDaemon(true);
```

### Race Condition Prevention
- **ObservableList:** Thread-safe for listeners
- **Platform.runLater():** Ensures UI updates on main thread
- **Single file path property:** No concurrent modification
- **Content cache:** Only read/written from main thread

---

## Configuration Storage Strategy

### Layered Fallback System

```
┌─────────────────────────────┐
│   PER-FILE STORAGE          │ ← Check first
│  highlights_<hash>.json     │
│  filters_<hash>.json        │
│  bookmarks_<hash>.json      │
├─────────────────────────────┤
│   GLOBAL STORAGE            │ ← Check second
│  highlights.json            │
│  filters.json               │
│  (no global bookmarks)      │
├─────────────────────────────┤
│   DEFAULTS                  │ ← Use if missing
│  Empty list                 │
│  Default patterns           │
│  Default rules              │
└─────────────────────────────┘
```

### File Hash Encoding

```java
// Convert file path to safe filename
String getFileHash(String filePath) {
    return Integer.toHexString(filePath.hashCode());
}

// Example
"/var/log/system.log" → "5f3a8c2e"
"highlights_5f3a8c2e.json"
```

**Why Hashing?**
- Prevents invalid filename characters
- Consistent across app sessions
- Allows path to change (hash stays same if content is same)

---

## Conclusion

The architecture balances:
- **Simplicity** - Easy to understand flow
- **Extensibility** - Managers are pluggable
- **Maintainability** - Clear separation of concerns
- **Performance** - Background threading prevents UI blocking
- **Persistence** - Flexible config system with fallbacks

Each component has a clear responsibility, reducing complexity and making the codebase maintainable for future enhancements.

---

**Last Updated:** 2026-02-26
