# Todo: Add Tags Column to Log Analysis Views

## Progress: 0/13 (0%)

## Tasks

### Implementation - PlaysTableModel
[ ] Add TagManager fields to PlaysTableModel
  File: [`src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysTableModel.java`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysTableModel.java)
  Action: Add `TagManager tagManager` and `List<String> tagNames` fields
  Reference: [`RegisteredTracksTableModel.java:289-312`](../../src/main/java/de/stationadmin/gui/track/RegisteredTracksTableModel.java:289-312)

[ ] Initialize TagManager in PlaysTableModel constructor
  Action: Add `tagManager = ClientContext.getAdminClient().getTagManager()` in constructor
  Action: Add `refreshTagNames()` method and PropertyChangeListener for tag updates

[ ] Add TAGS column to PlaysTableModel Column enum
  Action: Add TAGS after LISTENERS in Column enum
  Action: Add TAGS case to `getColumnName()` returning `textProvider.getString("play.column.tags")`
  Action: Add TAGS case to `getValueAt()` returning `getTags(play)`
  Action: Add TAGS case to `getColumnClass()` returning `String.class`

[ ] Add getTags method to PlaysTableModel
  Action: Add private `getTags(Play play)` method following RegisteredTracksTableModel pattern

### Implementation - PlaysViewer
[ ] Hide TAGS column by default in PlaysViewer
  File: [`src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java)
  Action: After line 96, add: `table.getColumnExt(table.convertColumnIndexToView(Column.TAGS.ordinal())).setVisible(false);`
  Reference: [`PlaysViewer.java:94-96`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java:94-96)

### Implementation - TrackTableModel
[ ] Add TagManager fields to TrackTableModel
  File: [`src/main/java/de/stationadmin/gui/loganalyzer/plays/TrackTableModel.java`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/TrackTableModel.java)
  Action: Add `TagManager tagManager` and `List<String> tagNames` fields
  Action: Add `TagManager` parameter to constructor
  Reference: [`RegisteredTracksTableModel.java:289-312`](../../src/main/java/de/stationadmin/gui/track/RegisteredTracksTableModel.java:289-312)

[ ] Initialize TagManager in TrackTableModel
  Action: Store TagManager parameter in field
  Action: Add `refreshTagNames()` method and PropertyChangeListener for tag updates

[ ] Add TAGS column to TrackTableModel Column enum
  Action: Add TAGS after LENGTH in Column enum
  Action: Add TAGS case to `getColumnName()` returning `textProvider.getString("play.column.tags")`
  Action: Add TAGS case to `getValueAt()` returning `getTags(track)`
  Action: Add TAGS case to `getColumnClass()` returning `String.class`

[ ] Add getTags method to TrackTableModel
  Action: Add private `getTags(RegisteredTrack track)` method following RegisteredTracksTableModel pattern

### Implementation - UnplayedTracksViewer
[ ] Pass TagManager to TrackTableModel constructor
  File: [`src/main/java/de/stationadmin/gui/loganalyzer/plays/UnplayedTracksViewer.java`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/UnplayedTracksViewer.java)
  Action: Modify TrackTableModel instantiation (line ~122) to pass `ClientContext.getAdminClient().getTagManager()`

[ ] Hide TAGS column by default in UnplayedTracksViewer
  Action: After table initialization, add: `table.getColumnExt(table.convertColumnIndexToView(Column.TAGS.ordinal())).setVisible(false);`
  Reference: [`PlaysViewer.java:94-96`](../../src/main/java/de/stationadmin/gui/loganalyzer/plays/PlaysViewer.java:94-96)

### Implementation - Message Bundles
[ ] Add message bundle entries
  File: [`src/main/resources/messagebundle.properties`](../../src/main/resources/messagebundle.properties)
  Action: Add `play.column.tags = Tags`
  
[ ] Add English message bundle entry
  File: [`src/main/resources/messagebundle_en.properties`](../../src/main/resources/messagebundle_en.properties)
  Action: Add `play.column.tags = Tags`

### Verification
[ ] Manual test PlaysViewer
  Action: Open PlaysViewer → verify TAGS column hidden by default
  Action: Enable TAGS column via column control → verify tags display correctly
  Expected: Tracks without tags show empty, tracks with tags show comma-separated list

## Done When
[ ] All 13 tasks checked
[ ] TAGS column appears in both PlaysViewer and UnplayedTracksViewer
[ ] TAGS column is hidden by default in both viewers
[ ] Tags display correctly as comma-separated values
[ ] Tracks without tags show empty/null values

## Validation
[ ] All build items from build.md have corresponding todos
[ ] Tasks ordered in logical dependency order
[ ] Every task has specific file and action
[ ] Manual verification steps included
