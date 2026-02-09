# Tail Logs - Project Documentation

## Project Overview

**Tail Logs** is a cross-platform **JavaFX desktop application** for monitoring and analyzing log files in real-time. It serves as a modern GUI alternative to the Unix `tail` command with advanced features like pattern-based highlighting, content filtering, bookmarking, and per-file configuration management.

**Type:** Log Viewer / Real-time Log Monitor
**Language:** Java 17
**UI Framework:** JavaFX 21.0.6 with RichTextFX
**Build Tool:** Maven
**Version:** 1.0-SNAPSHOT
**Total Code:** 3,652 lines across 21 Java files

---

## Architecture Overview

### Design Pattern: MVC + Manager Pattern

The application uses a hybrid architecture combining:
- **MVC for UI:** Controllers manage scene logic and user interactions
- **Manager Pattern for Business Logic:** Three independent managers (Highlight, Filter, Bookmark) handle domain-specific operations with loose coupling
- **Configuration Pattern:** Centralized PreferencesManager handles all persistence

### High-Level Architecture Diagram

```
┌─────────────────────────────────────┐
│      MainApplication (Entry)        │
│     (JavaFX Scene Management)       │
└────────────┬────────────────────────┘
             │
      ┌──────┴──────┐
      ▼             ▼
   Home       Application
   Scene       Scene
             │
      ┌──────┼──────┬───────────┐
      ▼      ▼      ▼           ▼
   MenuBar  CodeArea  RightPanel  StatusBar
            │         (Tabs)
      ┌─────┴────────────────────┐
      ▼      ▼      ▼            ▼
   Highlight Filter Bookmark   Manager
   Manager   Manager Manager    Classes
      │      │      │
      └──────┴──────┴─────────────┐
                                  ▼
                      PreferencesManager
                      (JSON Persistence)
                             │
                             ▼
                      ~/.tail_logs/
                      (Config Files)
```

---

## Core Components

### 1. UI Controllers (screens package)

#### **ApplicationController.java** (947 lines)
Main application logic controller for the log viewer interface.
- **Responsibilities:**
  - Manages the CodeArea (text display) for log content
  - Handles file opening/switching with tab support
  - Manages search functionality with real-time filtering
  - Integrates all three managers (Highlight, Filter, Bookmark)
  - Updates UI based on file selection and configuration changes
  - Coordinates tailing, pausing, and clearing of files

- **Key Methods:**
  - `setCurrentFile(File)` - Switches active file, loads per-file configs
  - `loadFile(File)` - Loads file content into CodeArea
  - `searchContent()` - Applies search filters and highlighting
  - `filterContent()` - Applies all filters with AND logic
  - `applyHighlighting()` - Updates CodeArea styles dynamically

#### **RightPanelController.java** (357 lines)
Manages the right-side panel with three tabs.
- **Tab 1 - Highlights Tab:**
  - Add/edit custom highlight patterns (text or regex)
  - Color picker for each pattern
  - Enable/disable toggle per pattern
  - Pattern list with delete functionality

- **Tab 2 - Filters Tab:**
  - Add/edit filter rules (text or regex)
  - Enable/disable toggle per rule
  - Filter rule list with delete functionality
  - AND logic combining all active filters

- **Tab 3 - Bookmarks Tab:**
  - Display bookmarked lines with line numbers
  - Line preview text
  - Navigate to bookmarked line
  - Delete bookmarks

#### **HomeController.java** (104 lines)
Home screen for initial file/folder selection.
- File and folder selection buttons
- Recent files list navigation

#### **SettingsController.java** (140 lines)
Settings dialog for appearance customization.
- Font size adjustment
- Font weight selection
- Background color customization

---

### 2. Manager Classes (managers package)

#### **HighlightManager.java** (399 lines)
Manages all highlighting logic with pattern-based and log-level highlighting.

**Features:**
- Custom pattern highlighting (plain text or regex)
- Log-level based highlighting (ERROR, WARN, INFO)
- Per-file pattern storage with global fallback
- Merging with priority: search > custom patterns > log levels
- Dynamic CSS stylesheet generation with versioning

**Key Methods:**
- `addHighlightPattern(pattern, color, isRegex)` - Add custom pattern
- `removeHighlightPattern(uuid)` - Delete pattern by ID
- `togglePattern(uuid, enabled)` - Enable/disable pattern
- `getPatterns()` - Get all current patterns
- `setCurrentFile(file)` - Load per-file patterns
- `applyCombinedHighlighting(content, codeArea)` - Apply all styles to display

**Data Flow:**
1. Patterns stored in memory as ObservableList
2. On application of highlighting:
   - Generate regex from all active patterns
   - Create CSS file with color mappings (versioned to bust cache)
   - Apply StyleSpans to CodeArea for real-time rendering
3. Search highlighting overlays on top with highest priority

**Storage:**
- Global: `~/.tail_logs/highlights.json`
- Per-file: `~/.tail_logs/highlights_<fileHash>.json`

#### **FilterManager.java** (169 lines)
Manages content filtering with AND logic across all rules.

**Features:**
- Multiple filter rules with AND logic (all must match)
- Plain text and regex pattern support
- Per-file rule storage with global fallback
- Preserves original line numbers after filtering
- Toggle enable/disable per rule

**Key Methods:**
- `addFilterRule(pattern, isRegex)` - Add filter rule
- `removeFilterRule(uuid)` - Delete rule
- `toggleRule(uuid, enabled)` - Enable/disable rule
- `getRules()` - Get all rules
- `setCurrentFile(file)` - Load per-file rules
- `filterContent(content)` - Apply all filters and return filtered text
- `matchesFilters(line)` - Check if single line matches all filters

**Filtering Logic:**
- Each line is tested against all active rules
- Line must match ALL rules to be included (AND logic)
- Original line numbers preserved in output format: `[lineNum] content`

**Storage:**
- Global: `~/.tail_logs/filters.json`
- Per-file: `~/.tail_logs/filters_<fileHash>.json`

#### **BookmarkManager.java** (74 lines)
Manages per-file bookmarks with metadata.

**Features:**
- Store bookmarks with line number and preview text
- Timestamp tracking for each bookmark
- ObservableList for UI binding
- Per-file storage

**Key Methods:**
- `addBookmark(lineNumber, linePreview)` - Add bookmark
- `removeBookmark(uuid)` - Delete by ID
- `clearAll()` - Clear all bookmarks
- `setCurrentFile(file)` - Load per-file bookmarks
- `getBookmarks()` - Get observable list for UI

**Storage:**
- Per-file only: `~/.tail_logs/bookmarks_<fileHash>.json`
- One file per bookmarked file

---

### 3. Model Classes (models package)

#### **HighlightPattern.java**
```java
uuid: String              // Unique ID
pattern: String           // Regex or text pattern
color: String             // Hex color code (#RRGGBB)
isRegex: boolean          // Pattern type flag
enabled: boolean          // Active/inactive toggle
```

#### **FilterRule.java**
```java
uuid: String              // Unique ID
pattern: String           // Regex or text pattern
isRegex: boolean          // Pattern type flag
enabled: boolean          // Active/inactive toggle
```

#### **Bookmark.java**
```java
uuid: String              // Unique ID
lineNumber: int           // Line number in file
linePreview: String       // First 100 chars of line
timestamp: long           // Creation timestamp (ms)
```

#### **RecentFile.java**
```java
filePath: String          // Absolute file path
timestamp: long           // Last accessed time (ms)
```

---

### 4. Configuration Management (config package)

#### **PreferencesManager.java** (277 lines)
Centralized configuration and persistence manager.

**Responsibilities:**
- Load/save appearance settings
- Per-file configuration (highlights, filters, bookmarks)
- Recent files tracking (max 10 files)
- Directory management: `~/.tail_logs/`

**Configuration Files:**
```
~/.tail_logs/
├── preferences.txt                # Appearance settings
├── highlights.json                # Global highlight patterns
├── highlights_<hash>.json         # Per-file highlights
├── filters.json                   # Global filter rules
├── filters_<hash>.json            # Per-file filters
├── bookmarks_<hash>.json          # Per-file bookmarks
├── recent_files.json              # Recent file list (max 10)
└── taillogs_highlights_v*.css     # Dynamic CSS stylesheets
```

**Key Methods:**
- `saveAppearanceSettings(settings)` - Save theme/font preferences
- `loadAppearanceSettings()` - Load theme/font preferences
- `saveHighlightPatterns(filePath, patterns)` - Save patterns for file
- `loadHighlightPatterns(filePath)` - Load patterns for file
- `getGlobalHighlightPatterns()` - Load global fallback patterns
- `saveFilterRules(filePath, rules)` - Save rules for file
- `loadFilterRules(filePath)` - Load rules for file
- `getGlobalFilterRules()` - Load global fallback rules
- `addRecentFile(filePath)` - Track file access
- `loadRecentFiles()` - Get recent file list
- `removeRecentFile(filePath)` - Remove from recent list

**Per-File vs Global Storage:**
- Each manager checks per-file storage first
- Falls back to global storage if per-file doesn't exist
- File path converted to hash via `encodeFileKey()` for safe naming

#### **AppearanceSettings.java** (53 lines)
Data class for theme customization.
```java
fontSize: int             // Font size in points
fontWeight: String        // "NORMAL", "BOLD", etc.
backgroundColor: String   // Hex color for editor background
```

#### **AppConfig.java** (68 lines)
Application-wide configuration constants.
- Directory paths
- File naming patterns
- Default values
- Version strings

---

### 5. Utility Classes (utils package)

#### **FileOperations.java** (243 lines)
Core file I/O and real-time tailing.

**Key Features:**
- Load file content (handles large files)
- Real-time tailing with background thread monitoring
- Pause/resume tailing capability
- Configurable check interval (default 1 second)

**Key Methods:**
- `loadFileContent(File)` - Read entire file
- `startTailing(File, callback)` - Begin monitoring for changes
- `stopTailing()` - Stop monitoring
- `pauseTailing()` / `resumeTailing()` - Pause without stopping thread
- `isFileBeingTailed()` - Check current tailing status

**Tailing Implementation:**
- Background thread monitors file size at regular intervals
- When size changes, reads new content from last known position
- Invokes callback with new lines via `Platform.runLater()` for thread safety
- Prevents blocking UI thread during large file operations

#### **SyntaxHighlighter.java** (68 lines)
Log-level based syntax highlighting.
- Detects ERROR, WARN, INFO keywords in lines
- Returns StyleSpans for syntax coloring
- Integrates with HighlightManager for combined highlighting

#### **FontStylesUtil.java** (54 lines)
Font and style management utilities.
- Font creation with size and weight
- Style string generation from settings

---

### 6. UI Creation (ui package)

#### **MenuBarCreator.java** (160 lines)
Dynamically creates application menu bar.

**Menus:**
- **File:** Open file, Open folder, Recent Files (dynamic), Exit
- **Tools:** Settings, Clear recent files
- **Help:** About, Documentation

**Recent Files Menu:**
- Automatically updated when files opened
- Up to 10 most recent files
- Click to reopen file
- Remove individual files from recent list

---

### 7. Main Application Entry Points

#### **MainApplication.java** (268 lines)
JavaFX Application main class.
- Initializes scene switching (Home → Application)
- Sets up main window with CSS styling
- Handles window close events
- Manages application lifecycle

#### **Launcher.java** (9 lines)
Entry point for JAR execution (created by Maven Shade plugin).
- Delegates to MainApplication

---

## Per-File vs Global Configuration

### How Per-File Settings Work

Each file maintains **completely separate** highlights, filters, and bookmarks. The system is designed so that:

1. **Per-File Storage Takes Precedence**
   - When you open File A and add highlight patterns, they're saved to `~/.tail_logs/highlights_<fileHashA>.json`
   - When you open File B, it loads `~/.tail_logs/highlights_<fileHashB>.json` (different file = different patterns)
   - File A's patterns are never applied to File B

2. **Manager Context Switching**
   - Each manager (HighlightManager, FilterManager, BookmarkManager) maintains a `currentFilePath` property
   - When switching files, calling `manager.setCurrentFile(filePath)` triggers:
     - `loadPatterns()` / `loadRules()` / `loadBookmarks()` for the new file
     - Clearing the old file's settings from memory
     - Populating the right panel UI with the new file's settings

3. **File Switching Points**
   - **Initial file open** (ApplicationController.setCurrentFile, line 370)
   - **Tab click** (ApplicationController.createTab, line 920) - **BUG FIX APPLIED HERE**
   - **File closing** (ApplicationController.closeFile, line 753)
   - **Folder open** (ApplicationController.setCurrentFolder, line 391)

### The Per-File Tab Switching Bug (Fixed)

**Previous Issue:** When clicking on a different file tab, the managers weren't updated with the new file's context. This caused:
```
❌ BEFORE FIX:
User clicks File B tab → currentFilePath = fileB → loadCurrentFile() called
BUT: highlightManager.currentFilePath still = fileA
Result: File B displays File A's highlight patterns!
```

**Fix Applied:** Added manager context switching before loading the file:
```
✅ AFTER FIX (lines 923-926):
User clicks File B tab → currentFilePath = fileB
→ highlightManager.setCurrentFile(fileB) [loads File B's patterns]
→ filterManager.setCurrentFile(fileB) [loads File B's rules]
→ bookmarkManager.setCurrentFile(fileB) [loads File B's bookmarks]
→ loadCurrentFile() [displays with correct settings]
Result: File B displays only its own patterns and rules!
```

### Storage Isolation

Each file gets its own configuration files:
```
~/.tail_logs/
├── highlights_<fileHashA>.json          # File A's patterns
├── highlights_<fileHashB>.json          # File B's patterns (separate!)
├── filters_<fileHashA>.json             # File A's rules
├── filters_<fileHashB>.json             # File B's rules (separate!)
├── bookmarks_<fileHashA>.json           # File A's bookmarks
└── bookmarks_<fileHashB>.json           # File B's bookmarks (separate!)
```

No cross-file contamination - each file's configuration is completely isolated.

---

## Data Flow Patterns

### 1. File Opening Workflow
```
User clicks "Open File"
    ↓
File chooser dialog
    ↓
ApplicationController.setCurrentFile(file)
    ↓
    ├→ HighlightManager.setCurrentFile(file)
    │   └→ Load per-file patterns or global fallback
    ├→ FilterManager.setCurrentFile(file)
    │   └→ Load per-file rules or global fallback
    ├→ BookmarkManager.setCurrentFile(file)
    │   └→ Load per-file bookmarks
    └→ FileOperations.loadFileContent(file)
        ├→ Read file content
        ├→ Apply filters & highlighting
        └→ Display in CodeArea

PreferencesManager.addRecentFile(file)
    └→ Save to recent_files.json (max 10)

MenuBarCreator.updateRecentFilesMenu()
    └→ Refresh Recent Files menu
```

### 2. Real-Time Tailing Workflow
```
ApplicationController.loadFile() calls FileOperations.startTailing(file)
    ↓
FileOperations spawns background thread
    ↓
Background thread loops:
  - Check file size every 1 second
  - If size increased: read new content
  - Platform.runLater() → trigger highlight callback
    ↓
Highlight callback invokes HighlightManager.applyCombinedHighlighting()
    ↓
    ├→ Apply custom patterns
    ├→ Apply log-level highlighting
    └→ Apply search highlighting (highest priority)
    ↓
CodeArea updated with new StyleSpans (visual refresh)
```

### 3. Search & Filter Workflow
```
User types in search box
    ↓
ApplicationController.searchContent() called
    ↓
    ├→ FilterManager.filterContent() → applies all filter rules
    └→ SearchHighlighter.applySearchHighlight() → highlights matches
    ↓
HighlightManager.applyCombinedHighlighting()
    ├→ Priority: search > custom patterns > log levels
    └→ Generate new CSS stylesheet (versioned)
    ↓
CodeArea updated with combined styles
```

### 4. Configuration Persistence Workflow
```
User adds highlight pattern
    ↓
HighlightManager.addHighlightPattern(pattern)
    └→ ObservableList.add() → triggers UI update

When file changes or app closes:
    ↓
PreferencesManager.saveHighlightPatterns(currentFile, patterns)
    ├→ Create file hash from path
    ├→ Serialize to JSON
    └→ Save to ~/.tail_logs/highlights_<hash>.json

On next file open:
    ↓
PreferencesManager.loadHighlightPatterns(file)
    ├→ Check per-file storage
    ├→ If not found, check global storage
    └→ Return patterns to HighlightManager
```

---

## Key Features

### Core Features
- ✅ Real-time log file tailing with background monitoring
- ✅ Multiple concurrent file monitoring with tab switching
- ✅ Syntax highlighting for log levels (ERROR, WARN, INFO)
- ✅ Live search with real-time filtering
- ✅ Content caching per file
- ✅ Pause/Resume tailing control
- ✅ Clear display and refresh functionality

### Advanced Features (Recently Implemented)
- ✅ Custom pattern highlighting (regex or plain text) with custom colors
- ✅ Multi-rule content filtering with AND logic
- ✅ Bookmarking with line numbers and preview text
- ✅ Per-file configuration persistence (highlights, filters, bookmarks)
- ✅ Recent files menu with quick access (max 10 files)
- ✅ Appearance customization (font size, weight, background color)
- ✅ Global/per-file storage with automatic fallback
- ✅ Automatic recent file tracking

### UI Features
- ✅ Tab-based interface for multiple open files
- ✅ Right panel with 3 tabs (Highlights, Filters, Bookmarks)
- ✅ Menu bar with File, Tools, Help menus
- ✅ Status bar showing file info and statistics
- ✅ Search bar with real-time filtering
- ✅ Control buttons (Pause, Clear, Refresh)

### Future Enhancement Opportunities
- 🔲 Bookmark "Go" button navigation not yet wired
- 🔲 Line number clicking for quick bookmarking
- 🔲 Right panel toggle button
- 🔲 Context menus (copy, bookmark from menu)
- 🔲 Keyboard shortcut customization
- 🔲 Drag-and-drop tab reordering
- 🔲 Export/import configuration
- 🔲 Multiple workspace profiles
- 🔲 Line number display in margin
- 🔲 Color scheme themes

---

## Technology Stack

### Core Technologies
- **JavaFX 21.0.6** - Cross-platform UI framework
  - Controls for buttons, menus, text areas
  - FXML for XML-based UI layout
  - CSS styling support
  - Scene graph and animation

- **RichTextFX 0.11.2** - Advanced text editing component
  - CodeArea for syntax-highlighted text display
  - StyleSpans for per-character styling
  - Efficient rendering for large documents

- **Gson 2.10.1** - JSON serialization/deserialization
  - Configuration persistence
  - Type-safe JSON mapping

### Supporting Libraries
- **ControlsFX 11.2.1** - Extended JavaFX controls (dialogs, notifications)
- **Ikonli/BootstrapFX** - Icon library and Bootstrap CSS styling
- **TilesFX 21.0.9** - Tile-based dashboard components
- **JUnit 5.12.1** - Unit testing framework

### Build & Deployment
- **Maven 3.x** - Build automation
  - Maven Compiler Plugin 3.13.0
  - JavaFX Maven Plugin 0.0.8
  - Maven Shade Plugin 3.5.0 (creates uber JAR: `log-tail.jar`)
- **Java 17** - Runtime and compilation target

---

## Build & Execution

### Building
```bash
# Clean build with packaging
mvn clean package
# Creates: target/log-tail.jar

# Quick rebuild (development)
mvn compile

# Skip tests (faster)
mvn clean package -DskipTests
```

### Running
```bash
# Development mode with hot-reload
mvn javafx:run

# JAR execution
java -jar target/log-tail.jar

# Convenience scripts
./run.sh      # Build and run
./build.sh    # Build only
```

### Maven Wrapper (Included)
```bash
# On Linux/Mac
./mvnw clean package

# On Windows
mvnw.cmd clean package
```

---

## Directory Structure

```
tail_logs/
├── pom.xml                                       # Maven configuration
├── Claude.md                                     # This file
├── IMPLEMENTATION_SUMMARY.md                     # Feature summary
├── build.sh / run.sh                            # Build scripts
├── mvnw / mvnw.cmd                              # Maven wrapper
│
├── src/main/java/org/taillogs/taillogs/
│   ├── MainApplication.java                     # JavaFX entry point (268 lines)
│   ├── Launcher.java                            # JAR entry point (9 lines)
│   │
│   ├── screens/                                 # UI Controllers
│   │   ├── ApplicationController.java           # Main app logic (947 lines)
│   │   ├── RightPanelController.java            # Right panel tabs (357 lines)
│   │   ├── HomeController.java                  # File selection (104 lines)
│   │   ├── SettingsController.java              # Settings dialog (140 lines)
│   │   └── MainController.java                  # Legacy (14 lines)
│   │
│   ├── managers/                                # Business Logic
│   │   ├── HighlightManager.java                # Pattern highlighting (399 lines)
│   │   ├── FilterManager.java                   # Content filtering (169 lines)
│   │   └── BookmarkManager.java                 # Bookmarks (74 lines)
│   │
│   ├── models/                                  # Data Models
│   │   ├── HighlightPattern.java
│   │   ├── FilterRule.java
│   │   ├── Bookmark.java
│   │   └── RecentFile.java
│   │
│   ├── config/                                  # Configuration
│   │   ├── PreferencesManager.java              # Persistence (277 lines)
│   │   ├── AppearanceSettings.java
│   │   └── AppConfig.java
│   │
│   ├── utils/                                   # Utilities
│   │   ├── FileOperations.java                  # File I/O & tailing (243 lines)
│   │   ├── SyntaxHighlighter.java               # Log highlighting (68 lines)
│   │   └── FontStylesUtil.java                  # Font utilities (54 lines)
│   │
│   └── ui/
│       └── MenuBarCreator.java                  # Menu generation (160 lines)
│
├── src/main/resources/org/taillogs/taillogs/
│   ├── app-view.fxml                           # Main window layout
│   ├── home-view.fxml                          # Home screen
│   ├── right-panel-view.fxml                   # Right panel layout
│   ├── settings-view.fxml                      # Settings dialog
│   └── styles.css                              # Global CSS styling
│
└── target/
    └── log-tail.jar                            # Executable JAR
```

---

## Configuration Files (User's Machine)

Location: `~/.tail_logs/`

```
~/.tail_logs/
├── preferences.txt                    # Appearance settings
│   Example: { fontSize: 12, fontWeight: "NORMAL", backgroundColor: "#FFFFFF" }
│
├── highlights.json                    # Global highlight patterns
│   [{ uuid: "...", pattern: "ERROR", color: "#FF0000", isRegex: false, enabled: true }, ...]
│
├── highlights_<fileHash>.json         # Per-file patterns (per-file takes precedence)
│
├── filters.json                       # Global filter rules
│   [{ uuid: "...", pattern: "WARN", isRegex: false, enabled: true }, ...]
│
├── filters_<fileHash>.json            # Per-file filter rules
│
├── bookmarks_<fileHash>.json          # Per-file bookmarks
│   [{ uuid: "...", lineNumber: 42, linePreview: "...", timestamp: 1234567890 }, ...]
│
├── recent_files.json                  # Recent file list (max 10)
│   [{ filePath: "/var/log/system.log", timestamp: 1234567890 }, ...]
│
└── taillogs_highlights_v<N>.css       # Dynamically generated stylesheets
    [.highlighted { -fx-fill: #FF0000; }, ...]
```

---

## Recent Changes & Git History

### Latest: Tab Switching Bug Fix (2026-02-09)

**Issue Identified & Fixed:**
- **Bug**: When switching between open file tabs, the HighlightManager and FilterManager retained the previous file's context, causing the old file's patterns and rules to apply to the new file
- **Root Cause**: Tab click handler (ApplicationController, line 920) was not calling `setCurrentFile()` on the managers
- **Fix**: Added manager context switching calls before loading the new file:
  ```java
  highlightManager.setCurrentFile(filePath);
  filterManager.setCurrentFile(filePath);
  bookmarkManager.setCurrentFile(filePath);
  ```
- **Impact**: Per-file highlights, filters, and bookmarks now work correctly when switching between tabs
- **Files Modified**: ApplicationController.java (lines 923-926)

### Previous: v1 - Per-File Configuration & Recent Files (Commit 5166448)

**Changes:** 846 insertions, 211 deletions across 12 files

**Features Added:**
1. **Per-File Storage** - Each file maintains separate highlights, filters, bookmarks
2. **Recent Files Menu** - Quick access to 10 most recently opened files
3. **Automatic Tracking** - Files automatically added to recent list on open
4. **Global Fallback** - Per-file configs fall back to global if not found
5. **File Hash Encoding** - Safe file naming using path hashing

**Modified Components:**
- `PreferencesManager` - Extended with per-file storage methods
- `HighlightManager` - Added per-file pattern loading and global fallback
- `FilterManager` - Added per-file rule loading and global fallback
- `ApplicationController` - Calls setCurrentFile() on file switch
- `MenuBarCreator` - Dynamic recent files menu generation
- `MainApplication` - Added recent file open callback

### Previous Commits
- **4096cce** - Button UI redesign
- **01e740e** - Filter feature implementation
- **ca11de9** - Text highlighting fixes with CSS versioning
- **bf063e5** - Gson serialization and highlight integration

---

## Project Status

**Status: PRODUCTION-READY** ✅

All core and advanced features are implemented and functional. The application successfully compiles, packages, and runs as a standalone JAR executable.

### Completed
- ✅ Real-time log tailing with concurrent file support
- ✅ Pattern-based highlighting with custom colors
- ✅ Multi-rule filtering with AND logic
- ✅ Line bookmarking with persistence
- ✅ Per-file configuration management
- ✅ Recent files tracking and quick access
- ✅ Appearance customization
- ✅ JSON persistence with fallback system
- ✅ Clean, modular architecture
- ✅ Proper error handling
- ✅ Build pipeline and packaging

### Recent Fixes
- ✅ **Fixed tab switching bug** (2026-02-09) - Tab switching now properly updates manager context to load per-file filters/highlights instead of applying previous file's settings

### Known Limitations
- Bookmark navigation ("Go" button) not wired
- Line number click bookmarking not implemented
- Right panel toggle not implemented
- Context menus not implemented
- Full keyboard shortcut support incomplete
- No drag-and-drop tab reordering
- No configuration import/export

---

## Development Notes

### Code Quality
- **Architecture:** Clean separation of concerns with Manager Pattern
- **Design:** Observable data binding for reactive UI updates
- **Threading:** Proper use of background threads for file operations
- **Storage:** Flexible per-file + global configuration system
- **Persistence:** JSON-based with automatic directory management
- **Error Handling:** Try-catch blocks on file operations and JSON parsing

### Architectural Strengths
1. **Loose Coupling** - Managers are independent and reusable
2. **Observable Data** - UI automatically syncs with data model changes
3. **Callback Architecture** - Components communicate through interfaces
4. **Fallback System** - Per-file storage with automatic global fallback
5. **Priority System** - Clear precedence for overlapping styles
6. **Async Operations** - File tailing doesn't block UI thread
7. **Dynamic CSS** - Stylesheet versioning prevents caching issues
8. **Type Safety** - Gson with type mapping for reliable serialization

### Future Enhancement Path
1. Implement bookmark navigation UI wiring
2. Add right panel toggle button
3. Implement keyboard shortcuts fully
4. Add context menus (copy, bookmark, search)
5. Create configuration import/export
6. Add theme profiles/workspace management
7. Implement line number margin display
8. Add drag-and-drop tab reordering

---

## Quick Reference

### Opening a File
1. Start application → `mvn javafx:run` or `java -jar log-tail.jar`
2. Click "Open File" or "Open Folder"
3. Select log file → File loads and begins tailing

### Adding a Highlight Pattern
1. Open file
2. Click "Highlights" tab in right panel
3. Enter pattern text (or regex)
4. Pick color
5. Click "Add Pattern"
6. Pattern automatically saved to `~/.tail_logs/highlights_<hash>.json`

### Creating a Filter
1. Open file
2. Click "Filters" tab in right panel
3. Enter filter pattern
4. Click "Add Filter"
5. Lines matching all filters displayed
6. Rule automatically saved to `~/.tail_logs/filters_<hash>.json`

### Searching Text
1. Type in search bar at top
2. Matching lines highlighted in yellow
3. Search results overlay all other highlighting

### Creating Bookmarks
1. (Future: click line number)
2. Bookmarks appear in "Bookmarks" tab
3. Click bookmark to navigate (future implementation)
4. Delete button removes bookmark

### Customizing Appearance
1. Click "Tools" → "Settings"
2. Adjust font size, weight, background color
3. Changes saved immediately to `~/.tail_logs/preferences.txt`

---

## Contact & Support

For issues or feedback, refer to the project repository or contact the development team.

**Build:** `mvn clean package`
**Run:** `java -jar target/log-tail.jar` or `mvn javafx:run`
**Config:** `~/.tail_logs/` (auto-created)

---

**Last Updated:** 2026-02-09
**Version:** 1.0-SNAPSHOT
**Status:** Production Ready
