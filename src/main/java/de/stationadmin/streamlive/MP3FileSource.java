/**
 * 
 */
package de.stationadmin.streamlive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author korf
 *
 */
public class MP3FileSource implements MP3Source {
  private File file;
  
  /**
   * @param file
   */
  public MP3FileSource(File file) {
    super();
    this.file = file;
  }

  
  @Override
  public InputStream getInputStream() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public String getLocation() {
    return file.getAbsolutePath();
  }


  /**
   * @return the file
   */
  public File getFile() {
    return file;
  }

}
