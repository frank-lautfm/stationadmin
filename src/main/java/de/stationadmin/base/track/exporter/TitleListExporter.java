package de.stationadmin.base.track.exporter;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.stationadmin.base.track.RegisteredTrack;

public interface TitleListExporter {

  void toFile(List<RegisteredTrack> titles, File file, boolean full) throws IOException;
}
