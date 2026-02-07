# Project Documentation Rules (Non-Obvious Only)

## Package Organization
- `de.stationadmin.base.*` - Core business logic (GUI-independent)
- `de.stationadmin.gui.*` - Swing GUI components
- `de.stationadmin.lfm.backend.*` - laut.fm API backend service
- `de.stationadmin.lfmapi.*` - laut.fm public API
- `org.json.*` - Bundled JSON library (not external)

## Build System
- Uses **Apache Ant**, not Maven or Gradle
- Build file: [`build.xml`](../../build.xml:1)
- Dependencies are in [`lib/`](../../lib/) subdirectories (not managed by build tool)

## Non-Standard Conventions
- Source encoding is ISO-8859-15 (not UTF-8) for German umlauts
- Test files exist but are excluded from compilation
- Property change listeners are transient (XStream serialization)
- Resources are split: some in JAR, some in external directories

## Key Files
- Main entry: [`de.stationadmin.gui.Start`](../../src/main/java/de/stationadmin/gui/Start.java:38)
- Base class for beans: [`AbstractBean`](../../src/main/java/de/stationadmin/base/util/AbstractBean.java:19)
- Config file: [`stationadmin-default.conf`](../../src/main/resources/stationadmin-default.conf:1)

## Documentation Locations
- User guide: [`doc/userguide/`](../../doc/userguide/)
- Changes: [`doc/changes.txt`](../../doc/changes.txt)
- License: [`doc/license.txt`](../../doc/license.txt)
- Javadoc: Generated to `doc/javadoc/` via `ant javadoc`

## Language
- Code comments are mixed German and English
- User-facing strings are in German (primary) and English
- Message bundles: [`messagebundle.properties`](../../src/main/resources/messagebundle.properties), [`messagebundle_de.properties`](../../src/main/resources/messagebundle_de.properties), [`messagebundle_en.properties`](../../src/main/resources/messagebundle_en.properties)
