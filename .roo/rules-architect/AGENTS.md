# Project Architecture Rules (Non-Obvious Only)

## Layer Separation
- **Critical**: `de.stationadmin.base.*` must not depend on `de.stationadmin.gui.*`
- Core business logic is GUI-independent for potential library use
- Build target `dist.lib` creates core-only JAR excluding GUI packages

## Property Change Pattern
- [`AbstractBean`](../../src/main/java/de/stationadmin/base/util/AbstractBean.java:19) provides property change support
- **Must call** `AbstractBean.setEventsInEDT(true)` at startup for Swing EDT safety
- Property change listeners are transient (not serialized by XStream)
- This pattern is used throughout for data binding

## Dependency Management
- Libraries are **not managed** by Maven/Gradle
- All dependencies in [`lib/`](../../lib/) subdirectories
- JAR classpath is built dynamically in [`build.xml:114-120`](../../build.xml:114)
- Key dependencies: SwingX, Velocity, XStream, OpenCSV, Log4j2

## Resource Distribution
- Application resources are split between JAR and external directories
- Velocity templates (`.vm`) → `conf/` directory
- Config files → specific directories (not in JAR)
- This allows user customization without rebuilding

## Serialization
- Uses XStream for object persistence
- Property change listeners are transient (not serialized)
- See [`XStreamFactory`](../../src/main/java/de/stationadmin/base/util/XStreamFactory.java) for configuration

## API Structure
- Two separate laut.fm API packages:
  - `de.stationadmin.lfm.backend.*` - Admin/backend API
  - `de.stationadmin.lfmapi.*` - Public API
- JSON handling via bundled `org.json.*` package

## Build Targets
- `ant compile` - Compile only
- `ant dist` - Full application distribution (default)
- `ant dist.lib` - Core library without GUI
- `ant dist.lib.raw` - Minimal library (no base or GUI)
- Each target has specific package exclusions

## Logging Architecture
- Log4j2 configured programmatically (no XML config)
- Configuration in [`Start.configureLogging()`](../../src/main/java/de/stationadmin/gui/Start.java:42)
- Rolling file appender with size-based rotation
- Logs to user home directory, not application directory
