# Build: Add Tags Column to Log Analysis Views

## Quick Summary
Add an optional TAGS column to PlaysViewer and UnplayedTracksViewer that displays comma-separated tags for each track, hidden by default.

## Files

Create:
- None

Modify:
- [`src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysTableModel.java`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysTableModel.java): Add TAGS column with tag retrieval logic
- [`src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java): Hide TAGS column by default (line ~96)
- [`src/main/java/de/stationadmin/gui/loganalyzer/plays/TrackTableModel.java`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/TrackTableModel.java): Add TAGS column with tag retrieval logic, add TagManager parameter to constructor
- [`src/main/java/de/stationadmin/gui/loganalyzer/plays/UnplayedTracksViewer.java`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/UnplayedTracksViewer.java): Pass TagManager to TrackTableModel constructor (line ~122)
- [`src/main/resources/messagebundle.properties`](../../src/main/resources/messagebundle.properties): Add `play.column.tags = Tags`
- [`src/main/resources/messagebundle_en.properties`](../../src/main/resources/messagebundle_en.properties): Add `play.column.tags = Tags`

## Implementation Steps

1. **Add TAGS column to PlaysTableModel**
   Reference: [`RegisteredTracksTableModel.java:289-312, 334`](../../src/main/java/de/stationadmin/gui/track/RegisteredTracksTableModel.java:289-312)
   - Add `TagManager tagManager` and `List<String> tagNames` fields
   - Initialize TagManager from `ClientContext.getAdminClient().getTagManager()` in constructor
   - Add `refreshTagNames()` method and PropertyChangeListener for tag updates
   - Add TAGS to Column enum (after LISTENERS)
   - Add TAGS case to `getColumnName()` returning `textProvider.getString("play.column.tags")`
   - Add TAGS case to `getValueAt()` returning `getTags(play)`
   - Add TAGS case to `getColumnClass()` returning `String.class`
   - Add private `getTags(Play play)` method following RegisteredTracksTableModel pattern

2. **Hide TAGS column in PlaysViewer**
   Reference: [`PlaysViewer.java:94-96`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java:94-96)
   - After line 96, add: `table.getColumnExt(table.convertColumnIndexToView(Column.TAGS.ordinal())).setVisible(false);`

3. **Add TAGS column to TrackTableModel**
   Reference: [`RegisteredTracksTableModel.java:289-312, 334`](../../src/main/java/de/stationadmin/gui/track/RegisteredTracksTableModel.java:289-312)
   - Add `TagManager tagManager` and `List<String> tagNames` fields
   - Add `TagManager` parameter to constructor
   - Add `refreshTagNames()` method and PropertyChangeListener for tag updates
   - Add TAGS to Column enum (after LENGTH)
   - Add TAGS case to `getColumnName()` returning `textProvider.getString("play.column.tags")`
   - Add TAGS case to `getValueAt()` returning `getTags(track)`
   - Add TAGS case to `getColumnClass()` returning `String.class`
   - Add private `getTags(RegisteredTrack track)` method following RegisteredTracksTableModel pattern

4. **Update UnplayedTracksViewer to pass TagManager**
   - Modify TrackTableModel instantiation (line ~122) to pass `ClientContext.getAdminClient().getTagManager()`

5. **Hide TAGS column in UnplayedTracksViewer**
   Reference: [`PlaysViewer.java:94-96`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java:94-96)
   - After table initialization, add: `table.getColumnExt(table.convertColumnIndexToView(Column.TAGS.ordinal())).setVisible(false);`

6. **Add message bundle entries**
   - Add `play.column.tags = Tags` to both messagebundle.properties and messagebundle_en.properties

Manual Test:
1. Open PlaysViewer → verify TAGS column hidden by default → enable via column control → verify tags display
2. Open UnplayedTracksViewer → verify TAGS column hidden by default → enable via column control → verify tags display
3. Verify tracks without tags show empty/null, tracks with multiple tags show comma-separated list

## Validation
- [x] All files verified in workspace
- [x] Pattern reference confirmed: RegisteredTracksTableModel.java
- [x] Column visibility pattern confirmed: PlaysViewer.java:94-96
- [x] Dependencies confirmed: TagManager available via ClientContext
