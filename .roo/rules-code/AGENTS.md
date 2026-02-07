# Project Coding Rules (Non-Obvious Only)

## Critical Patterns

### Property Change Events
- Always call `AbstractBean.setEventsInEDT(true)` at application startup
- This is **mandatory** for Swing EDT thread safety (see [`Start.java:177`](../../src/main/java/de/stationadmin/gui/Start.java:177))
- Property change listeners are transient (not serialized by XStream)

### File Encoding
- **All source files must use ISO-8859-15 encoding** (not UTF-8)
- This is configured in [`build.xml:68`](../../build.xml:68) with `encoding="iso-8859-15"`
- Required for German umlauts in comments and strings

### Package Dependencies
- `de.stationadmin.base.*` packages **must not** depend on `de.stationadmin.gui.*`
- Core business logic is GUI-independent
- GUI components can depend on base packages

### Resource Files
- Velocity templates (`.vm`) go to `conf/` directory, not in JAR
- Config files (`stationadmin-default.conf`, `trackmapping`, `shufflescripts.json`) have specific target directories
- See [`build.xml:74-106`](../../build.xml:74) for resource handling rules

### Test Files
- Test files matching `**/Test*.java` are **excluded from compilation**
- Tests exist in [`src/main/test/`](../../src/main/test/) but are not part of default build
- No test framework is configured in the build

### Logging
- Log4j2 is configured programmatically, not via XML/properties files
- Configuration happens in [`Start.configureLogging()`](../../src/main/java/de/stationadmin/gui/Start.java:42)
- Logs go to `%USERPROFILE%/laut.fm/StationAdmin/stationadmin.log`

### JSON Library
- Uses bundled `org.json.*` package (not external dependency)
- Located in [`src/main/java/org/json/`](../../src/main/java/org/json/)
- Do not add external JSON libraries

## Code Style

- Author tags are inconsistent: `@author korf`, `@author Frank`, or `@author Frank Korf`
- German comments mixed with English code is normal
- Many `TODO` and `FIXME` comments exist (not blocking issues)
