/**
 * 
 */
package de.stationadmin.base.track.upload;

import java.io.File;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.lfm.backend.TrackUpload;
import de.stationadmin.lfm.backend.UploadResponse;

/**
 * @author korf
 *
 */
public class QueuedTrack {
  private TrackUpload file;
  private UploadResponse response;
  private DetailedTrack track;
  
  public QueuedTrack(File file) {
    this.file = new TrackUpload(file);
    if(file.length() < 1024 * 1024) {
      this.file.setPrivateTrack(true);
    }
  }

  /**
   * @return the track
   */
  public TrackUpload getFile() {
    return file;
  }

  /**
   * @param track the track to set
   */
  public void setFile(TrackUpload track) {
    this.file = track;
  }

  /**
   * @return the response
   */
  public UploadResponse getResponse() {
    return response;
  }

  /**
   * @param response the response to set
   */
  public void setResponse(UploadResponse response) {
    this.response = response;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.file.getFile().getName();
  }

  /**
   * @return the track
   */
  public DetailedTrack getTrack() {
    return track;
  }

  /**
   * @param track the track to set
   */
  public void setTrack(DetailedTrack track) {
    this.track = track;
  }

}
