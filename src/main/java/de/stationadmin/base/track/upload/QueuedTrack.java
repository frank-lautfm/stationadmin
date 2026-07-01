/**
 *
 */
package de.stationadmin.base.track.upload;

import java.io.File;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.util.TrackMetaDataReaderFactory;
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
  private UploadStatus status = UploadStatus.QUEUED;
  private boolean modified = false;
  /** Meta data read from the local file at queue time; may be null */
  private DetailedTrack localMetaData;

  public QueuedTrack(File file) {
    this.file = new TrackUpload(file);
    if(file.length() < 1024 * 1024) {
      this.file.setPrivateTrack(true);
    }
    this.localMetaData = TrackMetaDataReaderFactory.readMetaData(file);
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

  /**
   * @return the status
   */
  public UploadStatus getStatus() {
    return status;
  }

  /**
   * @param status the status to set
   */
  public void setStatus(UploadStatus status) {
    this.status = status;
  }

  /**
   * @return the modified
   */
  public boolean isModified() {
    return modified;
  }

  /**
   * @param modified the modified to set
   */
  public void setModified(boolean modified) {
    this.modified = modified;
  }

  /**
   * Returns the meta data that was read from the local file when this track
   * was added to the upload queue. May be <code>null</code> if the file
   * contained no readable tags or the format is not supported.
   *
   * @return local meta data, or <code>null</code>
   */
  public DetailedTrack getLocalMetaData() {
    return localMetaData;
  }

  /**
   * @param localMetaData the localMetaData to set
   */
  public void setLocalMetaData(DetailedTrack localMetaData) {
    this.localMetaData = localMetaData;
  }

}
