# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Build System

- **Build Tool**: Apache Ant (not Maven/Gradle)
- **Build File**: [`build.xml`](build.xml:1)
- **Java Version**: 1.8 (source and target)
- **Encoding**: ISO-8859-15 (not UTF-8!)

## Build Commands

```bash
# Compile only
ant compile

# Build distributable application (default target)
ant dist

# Build core library without GUI
ant dist.lib

# Clean build artifacts
ant clean

# Generate Javadoc
ant javadoc
```

## Critical Non-Obvious Patterns

### File Encoding
- **Source files use ISO-8859-15 encoding** (line 68 in [`build.xml`](build.xml:68))
- German umlauts in comments and strings require this encoding
- Config file [`stationadmin-default.conf`](src/main/resources/stationadmin-default.conf:1) contains German text with special characters

### Test Files
- Test files are **excluded from compilation** via `excludes="**/Test*.java"` (line 68 in [`build.xml`](build.xml:68))
- Tests are in [`src/main/test/`](src/main/test/) but not compiled by default build

### Resource Handling
- Velocity templates (`.vm` files) are **excluded from JAR** but copied to `conf/` directory
- `stationadmin-default.conf`, `trackmapping`, and `shufflescripts.json` go to specific directories, not in JAR

### Property Change Events
- [`AbstractBean`](src/main/java/de/stationadmin/base/util/AbstractBean.java:19) base class provides property change support
- **Critical**: Call `AbstractBean.setEventsInEDT(true)` at startup (line 177 in [`Start.java`](src/main/java/de/stationadmin/gui/Start.java:177))
- This ensures all property change events fire on Swing EDT

### Package Structure
- `de.stationadmin.base.*` - Core business logic (no GUI dependencies)
- `de.stationadmin.gui.*` - Swing GUI components
- `de.stationadmin.lfm.backend.*` - laut.fm API backend service
- `de.stationadmin.lfmapi.*` - laut.fm public API
- `org.json.*` - Bundled JSON library (not external dependency)

### Logging
- Uses Log4j2 configured programmatically in [`Start.configureLogging()`](src/main/java/de/stationadmin/gui/Start.java:42)
- Logs to `%USERPROFILE%/laut.fm/StationAdmin/stationadmin.log`
- Rolling file appender with 5MB max size, 10 backups

### Main Entry Point
- Main class: [`de.stationadmin.gui.Start`](src/main/java/de/stationadmin/gui/Start.java:38)
- Accepts optional command-line args: `token` and `stationName` for auto-login

### Dependencies
- Libraries in [`lib/`](lib/) subdirectories (not Maven/Gradle managed)
- SwingX, Velocity, XStream, OpenCSV are key dependencies
- Launch4j wrapper for Windows executable

## Code Style

- Author tags use `@author korf`, `@author Frank`, or `@author Frank Korf` inconsistently
- Many `TODO` and `FIXME` comments exist (not blocking)
- German comments mixed with English code
- Property change listeners are transient (not serialized by XStream)

## JavaScript scripts
src\main\javascript\shuffle contains JavaScript files with shuffle algorithms that are not part of the main Java application. Those are deployed separately to the laut.fm server. There are no local tools for building or testing them.