/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.io.File;

/**
 * @author korf
 *
 */
public class TrackUpload {

  private File file;
  private boolean privateTrack = false;

  /**
   * @param file
   */
  public TrackUpload(File file) {
    super();
    this.file = file;
  }
  
  public TrackUpload(String filename) {
    super();
    this.file = new File(filename);
  }


  /**
   * @return the file
   */
  public File getFile() {
    return file;
  }

  /**
   * @return the privateTrack
   */
  public boolean isPrivateTrack() {
    return privateTrack;
  }

  /**
   * @param privateTrack
   *          the privateTrack to set
   */
  public void setPrivateTrack(boolean privateTrack) {
    this.privateTrack = privateTrack;
  }

}
