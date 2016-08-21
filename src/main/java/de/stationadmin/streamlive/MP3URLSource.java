/**
 * 
 */
package de.stationadmin.streamlive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author korf
 *
 */
public class MP3URLSource implements MP3Source {
  private String location; 
  
  /**
   * @param location
   */
  public MP3URLSource(String location) {
    super();
    this.location = location;
  }


  @Override
  public InputStream getInputStream() throws IOException {
    return new URL(location).openStream();
  }

  @Override
  public String getLocation() {
    return this.location;
  }

}
