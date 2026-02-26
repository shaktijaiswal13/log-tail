# Resizable Right Panel Feature

## Overview

The right panel (Highlights, Filters, Bookmarks) is now **fully resizable** using a draggable divider. Users can click and drag the divider left or right to adjust the panel width.

---

## How to Use

### Resize the Right Panel

1. **Locate the divider** - A vertical line between the log display area and the right panel
2. **Position your cursor** - Move cursor to the divider (cursor changes to ↔️ resize arrow)
3. **Click and drag** - Drag left to shrink the right panel, drag right to expand it
4. **Release** - Position is maintained

### Panel Sizing

- **Minimum width:** 200px (right panel can't shrink below this)
- **Initial width:** 25% of window (75% log display, 25% right panel)
- **Maximum width:** Fills up to parent container
- **Divider width:** 8px (easy to grab and drag)

---

## Implementation Details

### FXML Changes

**Before:**
```xml
<!-- BorderPane with right region -->
<BorderPane>
    <center>
        <!-- Log area -->
    </center>
    <right>
        <VBox fx:id="rightPanelContainer" .../>
    </right>
</BorderPane>
```

**After:**
```xml
<!-- SplitPane with resizable divider -->
<center>
    <SplitPane dividerPositions="0.75">
        <!-- Left: Log area (75%) -->
        <VBox>...</VBox>

        <!-- Right: Right panel (25%) -->
        <VBox fx:id="rightPanelContainer" .../>
    </SplitPane>
</center>
```

### Key Changes

1. **Replaced BorderPane layout** with SplitPane in the center region
2. **Set divider position** to 0.75 (75% left, 25% right)
3. **Added minWidth="200"** to right panel (prevents collapse)
4. **Removed maxWidth** constraint (allows full expansion)
5. **Added CSS styling** for divider appearance and interaction

### CSS Styling

```css
.split-pane:horizontal .split-pane-divider {
    -fx-background-color: #eeeeee;
    -fx-cursor: col-resize;
    -fx-min-width: 8;
}

.split-pane:horizontal .split-pane-divider:hover {
    -fx-background-color: #d0d0d0;  /* Darker on hover */
}
```

---

## User Experience

### Visual Feedback

- **Normal state** - Light gray divider (#eeeeee)
- **Hover state** - Darker gray (#d0d0d0) to indicate it's interactive
- **Dragging** - Cursor changes to resize arrow (↔️)
- **Divider width** - 8px, easy to target

### Behavior

- **Smooth resizing** - Divider moves smoothly as you drag
- **Persistent position** - Position maintained during session (until user resizes again)
- **Responsive** - Works with window resizing
- **Touch-friendly** - 8px target is adequate for mouse and touchpad

---

## Technical Details

### SplitPane Properties

| Property | Value | Purpose |
|----------|-------|---------|
| `dividerPositions` | `0.75` | Initial split (75/25) |
| `Left panel` | `VBox` | Log display area |
| `Right panel` | `VBox (minWidth="200")` | Right panel (can't go below 200px) |

### JavaFX SplitPane Features Used

- ✅ **Built-in divider** - No custom code needed
- ✅ **Mouse interaction** - Drag to resize automatically
- ✅ **CSS styling** - Divider appearance customizable
- ✅ **Programmatic control** - Can set position via code if needed

---

## Files Modified

### 1. **app-view.fxml**
- Changed `<center>` from `<VBox>` to `<SplitPane>`
- Moved log area into left pane of SplitPane
- Moved right panel into right pane of SplitPane
- Added `minWidth="200"` to right panel
- Removed `maxWidth="400"` constraint

### 2. **styles.css**
- Added `.split-pane` styling
- Added `.split-pane:horizontal .split-pane-divider` styling
- Added hover and pressed states for visual feedback
- Set cursor to `col-resize` for resize indication

---

## Browser/Widget Compatibility

This feature uses **JavaFX's native SplitPane control**, which means:
- ✅ Works on Windows, macOS, Linux
- ✅ Standard JavaFX behavior (familiar to JavaFX developers)
- ✅ No external dependencies added
- ✅ Native look and feel

---

## Future Enhancements

### Potential Improvements
1. **Remember size preferences** - Save divider position to preferences.txt
2. **Double-click to collapse** - Quick toggle of right panel visibility
3. **Toggle button** - Dedicated button to show/hide right panel
4. **Keyboard shortcut** - Ctrl+Shift+P to toggle panel
5. **Multiple preset sizes** - Quick reset to default positions

### Example: Save Divider Position

```java
// In PreferencesManager
public void saveDividerPosition(double position) {
    // Save position to preferences
}

public double loadDividerPosition() {
    // Load and return saved position
}
```

Then in ApplicationController:
```java
// Load saved position or use default
splitPane.setDividerPositions(loadDividerPosition());

// Listen for changes
splitPane.setOnMouseReleased(event -> {
    saveDividerPosition(splitPane.getDividerPositions()[0]);
});
```

---

## Testing Checklist

- [x] Builds successfully (`mvn clean compile`)
- [x] Packages into JAR (`mvn package`)
- [x] UI renders correctly
- [x] Divider visible and interactive
- [x] Dragging works smoothly
- [x] Minimum width enforced (200px)
- [x] Resizing with window works
- [x] Right panel content (tabs) still functional
- [x] Search and filtering still work
- [x] Highlighting still works
- [x] Bookmarks still work

---

## Known Behavior

### Current Behavior
- Divider position resets on app restart (position not saved)
- Divider can be dragged to minimum of 200px right panel width
- Position is float between 0.0 and 1.0 (0.75 = 75% left)

### Planned Improvements
- Save divider position to preferences.txt
- Add toggle button to collapse/expand right panel
- Add keyboard shortcut for panel toggling

---

## Usage Examples

### Example 1: Expand Right Panel for More Details
1. Use normal log viewing (75/25 split)
2. Click divider and drag right
3. Expand right panel to 40% of width
4. View more of highlights/filters/bookmarks tabs
5. Drag back to reset

### Example 2: Minimize Right Panel During Search
1. Performing intensive search/filtering
2. Drag divider left to minimize right panel
3. Maximize log display area
4. Drag back to normal when done

### Example 3: Adjust for Different Tasks
- **Writing filters:** Drag right to expand filter tab
- **Adding highlights:** Drag right to see color picker
- **Viewing logs:** Drag left for maximum log visibility

---

## Commit Information

**Commit Message:** "Add resizable right panel with SplitPane divider"

**Changes:**
- Modified: `app-view.fxml` (replaced BorderPane center layout with SplitPane)
- Modified: `styles.css` (added SplitPane divider styling)
- Build: Successful ✅
- Package: Successful ✅

---

## Related Documentation

- See **Claude.md** for component details
- See **ARCHITECTURE.md** for layout architecture
- See **DEVELOPER_GUIDE.md** for making further changes

---

**Status:** ✅ Implemented and Tested
**Version:** 1.0
**Date:** 2026-02-26
