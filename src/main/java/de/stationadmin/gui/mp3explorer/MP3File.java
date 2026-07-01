/**
 * 
 */
package de.stationadmin.gui.mp3explorer;

import java.io.File;

import org.jaudiotagger.tag.Tag;


public class MP3File {
  File file;
  Tag tag;
  TrackStatus status = TrackStatus.UNRESOLVED;
  float size;

  /**
   * @return the file
   */
  public File getFile() {
    return file;
  }

  /**
   * @param file
   *          the file to set
   */
  public void setFile(File file) {
    this.file = file;
    if(file.exists()) {
      this.size = (float)file.length() / (1024 * 1024);
    }
  }

  /**
   * @return the tag
   */
  public Tag getTag() {
    return tag;
  }

  /**
   * @param tag
   *          the tag to set
   */
  public void setTag(Tag tag) {
    this.tag = tag;
  }

  /**
   * @return the status
   */
  public TrackStatus getStatus() {
    return status;
  }

  /**
   * @param status the status to set
   */
  public void setStatus(TrackStatus status) {
    this.status = status;
  }

  /**
   * @return the size
   */
  public float getSize() {
    return size;
  }

  /**
   * @param size the size to set
   */
  public void setSize(float size) {
    this.size = size;
  }
}
