# Project Debug Rules (Non-Obvious Only)

## Log Locations
- Main log: `%USERPROFILE%/laut.fm/StationAdmin/stationadmin.log`
- Rolling file appender: 5MB max size, 10 backups (`.log.1` through `.log.10`)
- Log configuration is programmatic in [`Start.configureLogging()`](../../src/main/java/de/stationadmin/gui/Start.java:42)

## Build Artifacts
- Compiled classes: `build/` directory
- Distribution: `dist/application/` for full app, `dist/library/` for core lib
- JAR file: `dist/application/StationAdmin.jar`

## Debugging Gotchas
- Property change events may fire on wrong thread if `AbstractBean.setEventsInEDT(true)` not called
- This causes Swing EDT violations that are hard to debug
- Check [`Start.java:177`](../../src/main/java/de/stationadmin/gui/Start.java:177) for proper initialization

## File Encoding Issues
- Source files use ISO-8859-15, not UTF-8
- If German umlauts display incorrectly, check IDE encoding settings
- Build uses `encoding="iso-8859-15"` in [`build.xml:68`](../../build.xml:68)

## Test Execution
- Test files are **not compiled** by default build
- Tests in [`src/main/test/`](../../src/main/test/) are excluded via `excludes="**/Test*.java"`
- No test runner is configured in Ant build

## Main Entry Point
- Main class: `de.stationadmin.gui.Start`
- Optional args: `token` and `stationName` for auto-login
- See [`Start.main()`](../../src/main/java/de/stationadmin/gui/Start.java:88)
