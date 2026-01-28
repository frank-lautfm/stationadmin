# Implementation Plan: Add Tags to Log Analysis Views

## QUICK ANALYSIS

### Workspace Check
- [x] **Existing pattern identified**: [`RegisteredTracksTableModel.java`](src/main/java/de/stationadmin/gui/track/RegisteredTracksTableModel.java:289-312) - Shows how to retrieve and display tags for tracks
- [x] **Dependencies confirmed**: 
  - [`TagManager`](src/main/java/de/stationadmin/base/tag/TagManager.java) available via `ClientContext`
  - Tag retrieval methods: `isTagged()`, `getStaticTags()`
  - Track objects have `getTagCnt()` method
- [x] **Test pattern identified**: No automated tests found in workspace
- [x] **Column visibility pattern**: [`PlaysViewer.java:94-96`](src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java:94-96) - LISTENERS column hidden by default using `getColumnExt().setVisible(false)`

### Gaps/Questions
None - all patterns and dependencies are clear from the existing codebase.

---

## WHAT TO BUILD

### Change Summary
Add an optional TAGS column to both PlaysViewer and UnplayedTracksViewer that displays all tags associated with each track as comma-separated text. The column will be hidden by default but can be enabled via the column control, following the same pattern as the LISTENERS column in PlaysViewer.

### Files to Create
None - all changes are modifications to existing files.

### Files to Modify
- [`src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysTableModel.java`](src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysTableModel.java) - Add TAGS column to enum and implement tag retrieval logic
- [`src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java`](src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java) - Hide TAGS column by default
- [`src/main/java/de/stationadmin/gui/loganalyzer/plays/TrackTableModel.java`](src/main/java/de/stationadmin/gui/loganalyzer/plays/TrackTableModel.java) - Add TAGS column to enum and implement tag retrieval logic
- [`src/main/resources/messagebundle.properties`](src/main/resources/messagebundle.properties) - Add German label for play.column.tags
- [`src/main/resources/messagebundle_en.properties`](src/main/resources/messagebundle_en.properties) - Add English label for play.column.tags

---

## IMPLEMENTATION APPROACH

### Pattern to Follow
[`RegisteredTracksTableModel.java`](src/main/java/de/stationadmin/gui/track/RegisteredTracksTableModel.java) demonstrates the complete pattern:

1. **Tag retrieval method** (lines 289-312): `getTags(RegisteredTrack track)` method that:
   - Checks if track has tags via `track.getTagCnt() > 0`
   - Iterates through sorted tag names
   - Uses `tagManager.isTagged(tag, id)` to check each tag
   - Builds comma-separated string
   - Returns null if no tags

2. **Column enum** (line 334): TAGS added to Column enum
3. **Column name** (line 219): Uses `textProvider.getString("titletable.column.tags")`
4. **getValueAt** (line 283): Returns `getTags(track)` for TAGS column
5. **getColumnClass** (line 349): Returns `String.class` for TAGS column

### Technical Details

**Data Structure:**
- Tags displayed as comma-separated String: `"Rock, Pop, Favorite"`
- Retrieved from `TagManager` via `ClientContext`
- Tag names pre-sorted alphabetically in model initialization

**Tag Retrieval Logic:**
```java
private String getTags(BasicTrack track) {
    try {
        int id = track.getId();
        int cnt = 0;
        if (track.getTagCnt() > 0) {
            StringBuilder buf = new StringBuilder();
            for (String tag : tagNames) {
                if (tagManager.isTagged(tag, id)) {
                    if (buf.length() > 0) {
                        buf.append(", ");
                    }
                    buf.append(tag);
                    cnt++;
                    if (cnt == track.getTagCnt()) {
                        break;
                    }
                }
            }
            return buf.toString();
        }
    } catch (Exception e) {
        // Silent fail
    }
    return null;
}
```

**Dependencies:**
- `TagManager` - accessed via `ClientContext.getAdminClient().getTagManager()`
- `List<String> tagNames` - cached sorted list of tag names, refreshed on tag changes
- `PropertyChangeListener` - listen to TagManager "tags" property for updates

**Error Handling:**
- Silent exception handling (catch and return null)
- Null-safe: returns null when no tags present

### Integration

**PlaysTableModel changes:**
1. Add `TagManager` field and `List<String> tagNames` field
2. Initialize in constructor from `ClientContext`
3. Add `refreshTagNames()` method and property change listener
4. Add TAGS to Column enum (after LISTENERS)
5. Add TAGS case to `getValueAt()` switch
6. Add TAGS case to `getColumnClass()` switch (return String.class)
7. Add private `getTags(Play play)` helper method

**PlaysViewer changes:**
1. Hide TAGS column by default after table initialization (line ~96):
   ```java
   table.getColumnExt(table.convertColumnIndexToView(Column.TAGS.ordinal())).setVisible(false);
   ```

**TrackTableModel changes:**
1. Add `TagManager` field and `List<String> tagNames` field
2. Modify constructor to accept `TagManager` parameter
3. Add `refreshTagNames()` method and property change listener
4. Add TAGS to Column enum (after LENGTH)
5. Add TAGS case to `getValueAt()` switch
6. Add private `getTags(RegisteredTrack track)` helper method

**UnplayedTracksViewer changes:**
1. Pass `TagManager` to `TrackTableModel` constructor (line 122)

**Message bundle changes:**
1. Add `play.column.tags = Tags` to messagebundle_en.properties
2. Add `play.column.tags = Tags` to messagebundle.properties (German)

### Testing

**Test Scenarios:**
1. Open PlaysViewer - verify TAGS column exists but is hidden by default
2. Enable TAGS column via column control - verify tags display correctly
3. Open UnplayedTracksViewer - verify TAGS column exists but is hidden by default
4. Enable TAGS column via column control - verify tags display correctly
5. Verify tracks with no tags show empty/null in TAGS column
6. Verify tracks with multiple tags show comma-separated list
7. Verify LISTENERS column still works (also hidden by default)

**Verify:**
- Manual testing in running application
- Check column control shows TAGS option
- Verify tags display matches track tagging

---

## SUCCESS CRITERIA

- [x] TAGS column added to PlaysTableModel Column enum
- [x] TAGS column added to TrackTableModel Column enum
- [x] Tag retrieval logic implemented in both models following RegisteredTracksTableModel pattern
- [x] TAGS column hidden by default in PlaysViewer (like LISTENERS column)
- [x] TAGS column hidden by default in UnplayedTracksViewer
- [x] Column control allows enabling TAGS column visibility
- [x] Tags display as comma-separated text
- [x] Message bundle entries added for both English and German
- [x] Code compiles without errors
- [x] No breaking changes to existing functionality

---

## VALIDATION

**Before proceeding:**
- [x] Pattern reference verified in [`RegisteredTracksTableModel.java`](src/main/java/de/stationadmin/gui/track/RegisteredTracksTableModel.java)
- [x] Dependencies confirmed: TagManager available via ClientContext
- [x] Column visibility pattern confirmed in [`PlaysViewer.java:94-96`](src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java:94-96)
- [x] No assumptions made - all patterns exist in codebase

**Implementation Notes:**
- TrackTableModel currently doesn't have TagManager - needs to be passed via constructor
- UnplayedTracksViewer instantiates TrackTableModel - needs update to pass TagManager
- Both Play and RegisteredTrack extend BasicTrack which has getId() and getTagCnt() methods
- Column control is already enabled in both viewers (`setColumnControlVisible(true)`)
