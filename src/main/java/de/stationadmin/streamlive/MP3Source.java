/**
 * 
 */
package de.stationadmin.streamlive;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author korf
 *
 */
public interface MP3Source {
  
  InputStream getInputStream() throws IOException;
  
  String getLocation();

}
