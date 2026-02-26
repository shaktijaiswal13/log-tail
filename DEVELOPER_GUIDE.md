# Tail Logs - Developer Guide

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.8.1 or higher
- Git (for version control)
- IDE: IntelliJ IDEA or VS Code with Java extensions (recommended)

### Setup Development Environment

```bash
# Clone the repository
git clone <repository-url>
cd tail_logs

# Install dependencies (Maven downloads them)
mvn clean install

# Verify setup
mvn --version
java -version  # Should be 17+
```

### Running in Development Mode

```bash
# Hot-reload development server
mvn javafx:run

# Or from IDE: Run → MainApplication.java
```

### Building for Distribution

```bash
# Create executable JAR
mvn clean package

# Output: target/log-tail.jar (can be distributed)
java -jar target/log-tail.jar
```

---

## Project Structure

### Directory Tree

```
tail_logs/
├── src/
│   ├── main/
│   │   ├── java/org/taillogs/taillogs/
│   │   │   ├── MainApplication.java      ← Entry point
│   │   │   ├── Launcher.java             ← JAR entry
│   │   │   ├── managers/                 ← Business logic
│   │   │   │   ├── HighlightManager.java
│   │   │   │   ├── FilterManager.java
│   │   │   │   └── BookmarkManager.java
│   │   │   ├── screens/                  ← UI Controllers
│   │   │   │   ├── ApplicationController.java
│   │   │   │   ├── RightPanelController.java
│   │   │   │   ├── HomeController.java
│   │   │   │   └── SettingsController.java
│   │   │   ├── models/                   ← Data classes
│   │   │   │   ├── HighlightPattern.java
│   │   │   │   ├── FilterRule.java
│   │   │   │   ├── Bookmark.java
│   │   │   │   └── RecentFile.java
│   │   │   ├── config/                   ← Configuration
│   │   │   │   ├── PreferencesManager.java
│   │   │   │   ├── AppearanceSettings.java
│   │   │   │   └── AppConfig.java
│   │   │   ├── utils/                    ← Utilities
│   │   │   │   ├── FileOperations.java
│   │   │   │   ├── SyntaxHighlighter.java
│   │   │   │   └── FontStylesUtil.java
│   │   │   └── ui/
│   │   │       └── MenuBarCreator.java
│   │   └── resources/
│   │       ├── org/taillogs/taillogs/
│   │       │   ├── app-view.fxml
│   │       │   ├── home-view.fxml
│   │       │   ├── right-panel-view.fxml
│   │       │   ├── settings-view.fxml
│   │       │   └── styles.css
│   └── test/                             ← Unit tests (future)
├── pom.xml                               ← Maven configuration
├── Claude.md                             ← Project documentation
├── ARCHITECTURE.md                       ← Architecture guide
├── PROJECT_STATUS.md                     ← Current status
├── IMPLEMENTATION_SUMMARY.md             ← Feature summary
├── MULTIPLE_FILES_FEATURE.md             ← Multi-file details
└── README.md                             ← Quick start
```

---

## Code Organization Principles

### Package Structure

```
org.taillogs.taillogs
├── screens       → UI Controllers (MVC View-Controller)
├── managers      → Business Logic (Manager Pattern)
├── models        → Data Models (POJO classes)
├── config        → Configuration & Persistence
├── utils         → Helper utilities
└── ui            → UI component creators
```

### Naming Conventions

| Component | Pattern | Example |
|-----------|---------|---------|
| Controllers | `*Controller` | `ApplicationController` |
| Managers | `*Manager` | `HighlightManager` |
| Models | `*` | `HighlightPattern` |
| Utils | `*Util` or `*Operations` | `FontStylesUtil` |
| FXML | `*-view.fxml` | `app-view.fxml` |

### Import Rules

```java
// ✅ DO: Import from clear packages
import org.taillogs.taillogs.managers.HighlightManager;
import org.taillogs.taillogs.models.HighlightPattern;

// ❌ DON'T: Use wildcard imports
import org.taillogs.taillogs.managers.*;
```

---

## Common Development Tasks

### Task 1: Adding a New Filter

**Scenario:** Add a "Case Sensitive" toggle to filter rules

**Steps:**

1. **Update Model (FilterRule.java):**
```java
public class FilterRule {
    private String uuid;
    private String pattern;
    private boolean isRegex;
    private boolean enabled;
    private boolean caseSensitive = false;  // ← ADD THIS

    // Add getter/setter
    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean cs) { this.caseSensitive = cs; }
}
```

2. **Update Manager (FilterManager.java):**
```java
private boolean matchesPattern(String line, FilterRule rule) {
    String pattern = rule.getPattern();
    String testLine = line;

    // Apply case sensitivity
    if (!rule.isCaseSensitive()) {
        pattern = pattern.toLowerCase();
        testLine = testLine.toLowerCase();
    }

    if (rule.isRegex()) {
        return testLine.matches(pattern);
    } else {
        return testLine.contains(pattern);
    }
}
```

3. **Update UI (right-panel-view.fxml or RightPanelController):**
```fxml
<!-- Add checkbox in filter list item -->
<CheckBox text="Case Sensitive"
    selected="${filterRule.caseSensitive}"/>
```

4. **Test:**
```bash
mvn clean compile
mvn javafx:run
# Test: Create filter, toggle case sensitivity, verify filtering
```

### Task 2: Adding Keyboard Shortcuts

**Scenario:** Add Ctrl+F for search focus

**Steps:**

1. **In ApplicationController.java:**
```java
@FXML
private void initialize() {
    // ... existing code ...

    // Add keyboard shortcut
    codeArea.setOnKeyPressed(event -> {
        if (event.isControlDown() && event.getCode() == KeyCode.F) {
            searchBar.requestFocus();
            event.consume();  // Consume to prevent default behavior
        }
    });
}
```

2. **Or use FXML (if using menu):**
```fxml
<MenuItem text="Find" onAction="#onFind">
    <accelerator>
        <KeyCodeCombination alt="DOWN" code="F" control="UP" meta="UP" shift="UP" shortcut="UP" />
    </accelerator>
</MenuItem>
```

3. **Test:**
```bash
mvn clean compile
mvn javafx:run
# Test: Press Ctrl+F in main window, search bar should focus
```

### Task 3: Adding a New Manager

**Scenario:** Add a SnippetManager for saving code snippets

**Steps:**

1. **Create Model (models/Snippet.java):**
```java
public class Snippet {
    private String uuid = UUID.randomUUID().toString();
    private String name;
    private String content;
    private long timestamp = System.currentTimeMillis();

    // Constructor, getters, setters...
}
```

2. **Create Manager (managers/SnippetManager.java):**
```java
public class SnippetManager {
    private final ObservableList<Snippet> snippets =
        FXCollections.observableArrayList();
    private String currentFilePath;

    public void setCurrentFile(String filePath) {
        this.currentFilePath = filePath;
        loadSnippets();
    }

    public void addSnippet(String name, String content) {
        Snippet snippet = new Snippet();
        snippet.setName(name);
        snippet.setContent(content);
        snippets.add(snippet);
        saveSnippets();
    }

    public ObservableList<Snippet> getSnippets() {
        return snippets;
    }

    private void loadSnippets() {
        // Load from PreferencesManager
        List<Snippet> loaded =
            PreferencesManager.getInstance()
                .loadSnippets(currentFilePath);
        snippets.clear();
        snippets.addAll(loaded);
    }

    private void saveSnippets() {
        PreferencesManager.getInstance()
            .saveSnippets(currentFilePath, new ArrayList<>(snippets));
    }
}
```

3. **Update PreferencesManager:**
```java
public void saveSnippets(String filePath, List<Snippet> snippets) {
    try {
        String fileKey = encodeFileKey(filePath);
        String filename = "snippets_" + fileKey + ".json";
        Gson gson = new Gson();
        String json = gson.toJson(snippets);
        // Write to ~/.tail_logs/snippets_<hash>.json
    } catch (IOException e) {
        System.err.println("Failed to save snippets: " + e.getMessage());
    }
}

public List<Snippet> loadSnippets(String filePath) {
    // Similar to loadHighlightPatterns()
}
```

4. **Wire in ApplicationController:**
```java
private SnippetManager snippetManager;

// In initialize()
snippetManager = new SnippetManager();
snippetManager.getSnippets().addListener((ListChangeListener<Snippet>) change -> {
    // UI update if needed
});
```

5. **Test:**
```bash
mvn clean compile
mvn javafx:run
# Verify snippets load/save
```

### Task 4: Fixing a Bug

**Example:** Search highlighting not clearing properly

**Debug Process:**

1. **Identify the Issue:**
   - Open a file
   - Type search text (highlighting works)
   - Clear search text (highlighting NOT cleared)

2. **Find the Code:**
```bash
grep -r "searchContent" src/
# Found in ApplicationController.java, line 612
```

3. **Read the Code:**
```java
private void searchContent() {
    String searchText = searchBar.getText().trim();

    if (searchText.isEmpty()) {
        // ← Bug might be here: not clearing?
    } else {
        // Apply search highlighting...
    }
}
```

4. **Understand the Bug:**
```java
if (searchText.isEmpty()) {
    // NOT calling reapplyHighlighting() - this is the bug!
}
```

5. **Fix It:**
```java
if (searchText.isEmpty()) {
    reapplyHighlighting();  // ← Add this
} else {
    // Apply search highlighting...
}
```

6. **Test:**
```bash
mvn clean compile
mvn javafx:run
# Test: Type search → Clear → Verify highlighting cleared
```

7. **Commit:**
```bash
git add -A
git commit -m "Fix search highlighting not clearing on empty search"
```

---

## Testing Best Practices

### Manual Testing

```bash
# Test scenario: Multi-file with filters
mvn clean package -DskipTests

java -jar target/log-tail.jar

# 1. Open file 1
# 2. Add highlight pattern
# 3. Open file 2
# 4. Add different highlight pattern
# 5. Switch between files → patterns should differ
# 6. Close file 1 → file 2 should become active
# 7. Exit and restart → settings should persist
```

### Unit Testing (Future)

```java
// Example test (src/test/java/...)
public class FilterManagerTest {

    @Test
    void testANDLogic() {
        FilterManager manager = new FilterManager();

        manager.addFilterRule("ERROR", false);
        manager.addFilterRule("system", false);

        String line1 = "ERROR in system module";
        String line2 = "ERROR in other module";

        assertTrue(manager.matchesFilters(line1));
        assertFalse(manager.matchesFilters(line2));
    }
}
```

Run tests:
```bash
mvn test
```

---

## Common Issues & Solutions

### Issue 1: "JavaFX runtime not found"

**Solution:**
```bash
# Check java version
java -version  # Should be 17+

# If needed, add JavaFX path
export JAVAFX_HOME=/path/to/javafx-sdk-21
mvn javafx:run
```

### Issue 2: FXML file not loading

**Solution:**
```bash
# Verify FXML exists
ls src/main/resources/org/taillogs/taillogs/*.fxml

# Check module-info.java has:
requires javafx.fxml;
requires javafx.controls;
opens org.taillogs.taillogs.screens to javafx.fxml;
opens org.taillogs.taillogs.ui to javafx.fxml;
```

### Issue 3: ObservableList not updating UI

**Solution:**
```java
// ✅ CORRECT: Use FXCollections
private ObservableList<Pattern> patterns =
    FXCollections.observableArrayList();

// ❌ WRONG: Plain ArrayList
private List<Pattern> patterns = new ArrayList<>();
```

### Issue 4: Search hangs on large files

**Solution:** Search runs on UI thread, for large files needs optimization:
```java
// Offload to background
Task<Void> searchTask = new Task<>() {
    protected Void call() throws Exception {
        // Do search on background thread
        return null;
    }
};
new Thread(searchTask).start();
```

---

## Code Style & Standards

### Java Style

```java
// ✅ Class declaration
public class MyClass {
    private final String value;  // ← Final when possible

    public MyClass(String value) {
        this.value = value;
    }
}

// ❌ Avoid
public class my_class {      // ← Wrong naming
    public String value;     // ← Not private
}
```

### JavaFX Style

```java
// ✅ Good: Proper initialization
@FXML
private void initialize() {
    // Initialize UI after FXML loads
}

// ✅ Good: Use Platform.runLater for thread safety
Platform.runLater(() -> {
    codeArea.appendText("New line");
});

// ❌ Bad: UI updates from background thread
Thread thread = new Thread(() -> {
    codeArea.appendText("This might crash");  // ← Dangerous
});
```

### Comments

```java
// ✅ Good: Explains WHY, not WHAT
// Use versioned CSS to bust browser cache issues
String cssFile = "highlights_v" + version + ".css";

// ❌ Bad: Explains WHAT (code is obvious)
// Loop through patterns
for (HighlightPattern pattern : patterns) {
    // ...
}
```

---

## Git Workflow

### Creating a Feature Branch

```bash
# Create and switch to new branch
git checkout -b feature/bookmark-navigation

# Work on feature...
# Make changes, commit frequently

# Push to remote
git push -u origin feature/bookmark-navigation

# Create pull request on GitHub
# Once approved, merge to main
```

### Commit Messages

```
✅ Good:
"Fix tab switching losing highlight context"
"Add keyboard shortcut Ctrl+F for search"
"Refine right panel settings load/save behavior"

❌ Bad:
"Fixed stuff"
"Changes"
"WIP"
```

### Typical Workflow

```bash
# 1. Create branch
git checkout -b feature/new-feature

# 2. Make changes
# ... edit files ...

# 3. Stage and commit
git add -A
git commit -m "Add new feature description"

# 4. Push
git push origin feature/new-feature

# 5. Create PR, get review, merge to main

# 6. Update local
git checkout main
git pull origin main
```

---

## Performance Optimization Tips

### 1. Lazy Loading

```java
// ✅ Load file content only when needed
private String cachedContent;

public String getContent() {
    if (cachedContent == null) {
        cachedContent = loadFromFile();  // Load once, cache
    }
    return cachedContent;
}
```

### 2. Batch Updates

```java
// ✅ Batch style updates instead of per-character
CodeArea.setStyle(0, content.length(),
    new ArrayList<>(styleSpans));  // One call

// ❌ Don't do per-character
for (int i = 0; i < content.length(); i++) {
    setStyleForIndex(i, style);  // Many calls, slow
}
```

### 3. Thread Management

```java
// ✅ Use daemon threads for background work
Thread worker = new Thread(() -> {
    // background work
});
worker.setDaemon(true);  // Auto-stop on app exit
worker.start();

// ✅ Stop threads cleanly
if (tailingThread != null && tailingThread.isAlive()) {
    tailingThread.interrupt();  // Signal to stop
}
```

---

## Deployment & Distribution

### Building Release JAR

```bash
# Clean build without tests
mvn clean package -DskipTests

# Output: target/log-tail.jar
# Size: ~50-60 MB (includes all dependencies)

# Test JAR
java -jar target/log-tail.jar
```

### Distribution Checklist

- [ ] All tests pass (`mvn test`)
- [ ] Code compiles without warnings (`mvn compile`)
- [ ] JAR builds successfully (`mvn package`)
- [ ] JAR runs correctly (`java -jar log-tail.jar`)
- [ ] All features tested manually
- [ ] Git history is clean
- [ ] Documentation updated
- [ ] Version updated in pom.xml (if applicable)

---

## Resources & References

### JavaFX Documentation
- Official: https://openjfx.io/
- Tutorials: https://docs.oracle.com/javase/8/javafx/

### RichTextFX
- GitHub: https://github.com/FXMisc/RichTextFX
- JavaDoc: https://fxmisc.github.io/richtext/javadoc/org/fxmisc/richtext/CodeArea.html

### Maven
- Guide: https://maven.apache.org/guides/
- Plugins: https://maven.apache.org/plugins/

### Git
- Guide: https://git-scm.com/book/
- Workflow: https://www.atlassian.com/git/workflows

---

## Quick Reference

### Build Commands
```bash
mvn clean compile          # Compile only
mvn clean package          # Build JAR
mvn javafx:run            # Dev run with hot-reload
mvn test                  # Run tests
mvn clean package -DskipTests  # Fast build
```

### File Locations
- **Source:** `src/main/java/org/taillogs/taillogs/`
- **Resources:** `src/main/resources/org/taillogs/taillogs/`
- **Config:** `~/.tail_logs/` (created at runtime)
- **JAR:** `target/log-tail.jar`

### Key Classes to Know
- `MainApplication.java` - App entry point
- `ApplicationController.java` - Main logic (947 lines)
- `HighlightManager.java` - Highlighting logic
- `FilterManager.java` - Filtering logic
- `PreferencesManager.java` - Config/persistence

---

**Last Updated:** 2026-02-26
**Status:** Complete & Production Ready
