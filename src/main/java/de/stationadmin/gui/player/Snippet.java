/**
 * 
 */
package de.stationadmin.gui.player;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.stationadmin.base.track.BasicTrack;

/**
 * @author korf
 *
 */
public class Snippet {
  private static Pattern pattern = Pattern.compile(".*\\w+\\_([\\w|_|-]+).mp3,\\d+", Pattern.DOTALL);

  private BasicTrack title;
  private URL url;
  private String source;
  
  public Snippet(BasicTrack title, URL url) {
    super();
    this.title = title;
    this.url = url;
    
    Matcher m = pattern.matcher(url.toString());
    if(m.matches()) {
      this.source = m.group(1);
    }
  }

  public BasicTrack getTitle() {
    return title;
  }

  public URL getUrl() {
    return url;
  }

  @Override
  public String toString() {
    return this.title.getArtist() + " - " + title.getTitle();
  }

  public String getSource() {
    return source;
  }

}
