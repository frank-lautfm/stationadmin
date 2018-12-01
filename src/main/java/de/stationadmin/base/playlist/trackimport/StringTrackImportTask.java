/**
 * 
 */
package de.stationadmin.base.playlist.trackimport;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.format.TrackExportFormat;

/**
 * Title import task that works with a string as input. A
 * {@link TrackExportFormat} must be given to parse the string.
 * 
 * @author Frank Korf
 */
public class StringTrackImportTask extends TrackImportTask {

  private TrackExportFormat format;
  private String source;

  public StringTrackImportTask(TrackExportFormat format, String source) {
    super();
    this.format = format;
    this.source = source;
  }

  /**
   * @see de.stationadmin.base.playlist.trackimport.TrackImportTask#getSourceString()
   */
  @Override
  public String getSourceString() {
    return this.source;
  }

  /**
   * @see de.stationadmin.base.playlist.trackimport.TrackImportTask#resolve()
   */
  @Override
  public void resolve() {
    BasicTrack track = this.format.fromString(this.source);
    if (track != null) {
      this.setArtist(track.getArtist());
      this.setTitle(track.getTitle());
      if (track instanceof DetailedTrack) {
        this.setAlbum(((DetailedTrack) track).getAlbum());
      }
      if (track.getId() >= 0) {
        this.setTrackLibraryTitle(track);
        this.setStatus(Status.RESOLVED);
      }
    } else {
      this.setStatus(Status.NO_TAGS);
    }

  }

}
