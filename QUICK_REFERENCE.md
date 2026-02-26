# Tail Logs - Quick Reference Card

## 🚀 Getting Started

### Build & Run
```bash
# Development (with hot-reload)
mvn javafx:run

# Build JAR
mvn clean package

# Run JAR
java -jar target/log-tail.jar

# Run convenience script
./run.sh
```

---

## 🎮 Application Usage

### File Operations
| Action | How |
|--------|-----|
| Open file | File → Open File (or Ctrl+O) |
| Open folder | File → Open Folder |
| Close file | Click ✕ on file tab |
| Switch file | Click file name in tab |
| Recent file | File → Recent Files → Click file |

### Display Control
| Action | Key/Button |
|--------|-----------|
| Pause tailing | Click "Pause" button |
| Resume tailing | Click "Resume" button |
| Clear display | Click "Clear" button |
| Refresh | Click "Refresh" button |

### Search & Filter
| Action | How |
|--------|-----|
| Search | Type in search box (top) |
| Clear search | Clear search box |
| Add filter | Filters tab → Enter pattern → Add |
| Remove filter | Filters tab → Click delete button |
| Clear all filters | Filters tab → Clear All button |

### Highlighting & Bookmarks
| Action | How |
|--------|-----|
| Add highlight | Highlights tab → Enter pattern → Pick color → Add |
| Remove highlight | Highlights tab → Click delete |
| Add bookmark | Bookmarks tab → (future: click line) |
| View bookmarks | Click "Bookmarks" tab |
| Delete bookmark | Bookmarks tab → Click delete |

### Settings
| Action | How |
|--------|-----|
| Font size | Tools → Settings → Adjust |
| Font weight | Tools → Settings → Bold/Normal |
| Background color | Tools → Settings → Color picker |
| Save settings | Click Apply |

---

## 📁 Configuration Files

### Location
```
~/.tail_logs/
```

### Files Created
```
preferences.txt                  # Appearance settings
highlights.json                  # Global highlight patterns
highlights_<fileHash>.json       # Per-file patterns
filters.json                     # Global filter rules
filters_<fileHash>.json          # Per-file rules
bookmarks_<fileHash>.json        # Per-file bookmarks
recent_files.json                # Recent file list
taillogs_highlights_v*.css       # Dynamic stylesheets
```

### Reset Configuration
```bash
# Remove all settings (back to defaults)
rm -rf ~/.tail_logs/

# Backup before deleting
cp -r ~/.tail_logs ~/.tail_logs.backup
```

---

## 🔧 Development Quick Commands

### Setup
```bash
# Clone and setup
git clone <repo>
cd tail_logs
mvn clean install
```

### Build Variants
```bash
mvn clean compile              # Compile only
mvn clean package              # Build JAR
mvn test                       # Run tests
mvn clean package -DskipTests  # Fast build (skip tests)
```

### Git Workflow
```bash
# Create feature branch
git checkout -b feature/name

# Check status
git status

# Stage and commit
git add -A
git commit -m "Description"

# Push to remote
git push origin feature/name
```

### Code Style Check
```bash
# Find TODO/FIXME comments
grep -r "TODO\|FIXME" src/

# Check imports
grep -r "import \*;" src/

# Find unused variables
grep "private.*;" src/ | grep -v "@"
```

---

## 📊 Project Structure (One-Liner Each)

### Source Packages
```
screens/              → UI Controllers (4 files, 1,548 lines)
managers/             → Business Logic (3 files, 642 lines)
models/               → Data Classes (4 files, ~150 lines)
config/               → Persistence (3 files, 398 lines)
utils/                → Helpers (3 files, 365 lines)
ui/                   → UI Creators (1 file, 160 lines)
```

### FXML Resources
```
app-view.fxml         → Main window layout
home-view.fxml        → File selection screen
right-panel-view.fxml → Right panel (Highlights/Filters/Bookmarks)
settings-view.fxml    → Settings dialog
styles.css            → Global styling
```

---

## 🐛 Common Issues & Quick Fixes

| Issue | Fix |
|-------|-----|
| `java: command not found` | Install Java 17+, add to PATH |
| `JavaFX runtime not found` | Ensure Java 17 is set as default |
| `FXML not loading` | Check module-info.java exports |
| `ObservableList not updating UI` | Use FXCollections.observableArrayList() |
| `Settings not persisting` | Check ~/.tail_logs/ permissions |
| `Search highlighting broken` | Verify pattern is valid regex |
| `Filter not working` | Check all rules enabled (AND logic) |
| `App hangs on large file` | File tailing on background thread, be patient |

---

## 📚 Key Classes at a Glance

### Entry Points
```java
MainApplication.java        // JavaFX app entry (268 lines)
Launcher.java              // JAR entry point (9 lines)
```

### Main Logic
```java
ApplicationController.java  // Core logic (947 lines) ⭐
RightPanelController.java   // Right panel tabs (357 lines)
```

### Business Logic
```java
HighlightManager.java       // Pattern highlighting (399 lines)
FilterManager.java          // Content filtering (169 lines)
BookmarkManager.java        // Bookmarks (74 lines)
```

### Configuration
```java
PreferencesManager.java     // Persistence (277 lines) 🔑
AppearanceSettings.java     // Theme data (53 lines)
```

### Utilities
```java
FileOperations.java         // File I/O & tailing (243 lines)
SyntaxHighlighter.java      // Log level colors (68 lines)
```

---

## 💾 Important File Paths

```
Source:          src/main/java/org/taillogs/taillogs/
FXML:            src/main/resources/org/taillogs/taillogs/
Config:          ~/.tail_logs/
JAR Output:      target/log-tail.jar
Build Config:    pom.xml
Module Config:   src/main/java/module-info.java
```

---

## 🔄 Data Flow (Simplified)

```
User Action
    ↓
Controller.onXxxClicked()
    ↓
Manager.addXxx() / removeXxx() / setCurrentFile()
    ↓
ObservableList updated
    ↓
UI automatically refreshed (via listener)
    ↓
PreferencesManager.saveXxx() (async/on change)
    ↓
JSON file written to ~/.tail_logs/
```

---

## 🧪 Testing Quick Commands

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=FilterManagerTest

# Run with coverage
mvn test jacoco:report

# Skip tests during build
mvn clean package -DskipTests
```

---

## 📈 Performance Tips

### For Large Files
- Open files: 500MB+ okay, may take 1-2s to load
- Search: <100ms for typical files
- Memory: ~100MB base + 5-10MB per file
- Tailing: Efficient, background thread

### Optimization
```java
// Good: Batch updates
CodeArea.setStyle(0, length, styleSpans);  // One call

// Bad: Per-character updates
for (int i = 0; i < length; i++) {
    updateStyle(i);  // Many calls, slow
}
```

---

## 🎯 Common Keyboard Shortcuts (Implemented)

| Shortcut | Action |
|----------|--------|
| Ctrl+O | Open File |
| Alt+F4 | Exit |
| (Future) Ctrl+F | Search |
| (Future) Ctrl+S | Save |

---

## 📝 Code Examples

### Adding Highlight Pattern
```java
HighlightManager manager = new HighlightManager();
manager.addHighlightPattern("ERROR", "#FF0000", false);
// Pattern is added, managers observers notified, UI updates
```

### Creating Filter
```java
FilterManager manager = new FilterManager();
manager.addFilterRule("WARN", false);
manager.addFilterRule("system", false);
// Both rules must match (AND logic)
```

### Loading Recent Files
```java
PreferencesManager prefs = PreferencesManager.getInstance();
List<RecentFile> recent = prefs.loadRecentFiles();
// Returns up to 10 most recent files with timestamps
```

---

## 🔐 Security Notes

- Configuration files: Plain JSON (not encrypted)
- No sensitive data stored (safe to back up)
- File operations: Safe from path traversal (uses file chooser)
- Serialization: Type-safe Gson (no arbitrary object deserialization)

---

## 🚢 Distribution

### Build Distribution Package
```bash
mvn clean package -DskipTests
# Output: target/log-tail.jar (~50-60 MB)
```

### System Requirements
- Java 17+
- 512MB RAM minimum
- 100MB disk space
- Any OS (Windows, macOS, Linux)

### Run on Any Machine
```bash
java -jar log-tail.jar
```

---

## 🔍 Debug Tricks

### Enable Debug Output
```bash
# Add to code:
System.out.println("Debug: " + variable);

# Then build and run
mvn javafx:run | grep Debug
```

### Check File Permissions
```bash
ls -la ~/.tail_logs/
# Should be rw- (600 or 644)
```

### Monitor File Changes
```bash
watch -n 1 ls -la ~/.tail_logs/
# Shows files updating in real-time
```

### JSON Validation
```bash
# Check JSON syntax
cat ~/.tail_logs/highlights.json | python -m json.tool
```

---

## 📞 Quick Help Commands

```bash
# Java version
java -version

# Maven version
mvn --version

# Maven help
mvn help:describe -Dplugin=org.openjfx:javafx-maven-plugin

# Find files
find src -name "*.java" | grep -i highlight

# Search code
grep -r "setCurrentFile" src/

# Count lines
find src -name "*.java" -exec wc -l {} + | tail -1
```

---

## ✅ Pre-Commit Checklist

Before pushing code:
```bash
# Compile without errors
mvn clean compile

# Run tests
mvn test

# Build JAR
mvn clean package

# Test JAR runs
java -jar target/log-tail.jar

# Check git status
git status

# Review changes
git diff

# Make commit
git commit -m "Clear message"

# Push
git push origin feature/name
```

---

## 📚 Documentation Quick Navigation

| Need | Document |
|------|----------|
| User guide | README.md |
| Setup dev | DEVELOPER_GUIDE.md |
| Architecture | ARCHITECTURE.md |
| Complete ref | Claude.md |
| Status & roadmap | PROJECT_STATUS.md |
| All docs | DOCUMENTATION_INDEX.md |

---

**Last Updated:** 2026-02-26
**Status:** Complete
**Version:** 1.0-SNAPSHOT

**Quick Start:**
```bash
git clone <repo> && cd tail_logs && mvn javafx:run
```
