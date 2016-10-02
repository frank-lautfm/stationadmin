/**
 * 
 */
package de.stationadmin.base.track.format;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.BasicTrack;

/**
 * 
 * @author Frank Korf
 * 
 */
public class CSVFormat implements TrackExportFormat {
  private static char[] separators = { ',', ';', '\t' };

  /**
   * @see de.stationadmin.base.track.format.TrackExportFormat#fromString(java.lang.String)
   */
  @Override
  public BasicTrack fromString(String str) {
    BasicTrack title = null;
    for (char separator : separators) {
      title = this.tryParse(str, separator);
      if (title != null) {
        return title;
      }
    }
    return title;
  }

  private BasicTrack tryParse(String str, int separator) {
    Position pos = new Position();
    
    DetailedTrack title = new DetailedTrack();
    title.setArtist(this.readNextValue(str, separator, pos));
    pos.pos++;
    title.setTitle(this.readNextValue(str, separator, pos));
    pos.pos++;
    title.setAlbum(this.readNextValue(str, separator, pos));

    return title.getTitle() != null ? title : null;
  }
  
  private String readNextValue(String str, int separator, Position p) {
    if(p.pos >= str.length()) {
      return null;
    }
    while(Character.isWhitespace(str.charAt(p.pos))) {
      p.pos++;
    }
    int pos = p.pos;
    boolean quoted = (str.charAt(pos) == '"');
    if(quoted) {
      p.pos++;
      boolean escape = false;
      StringBuilder value = new StringBuilder();
      while(p.pos < str.length() && !(str.charAt(p.pos) == '"' && escape == false)) {
        if(str.charAt(p.pos) == '\\' && !escape) {
          escape = true;
        }
        else {
          escape = false;
          value.append(str.charAt(p.pos));
        }
        p.pos++;
      }
      p.pos++;
      
      String vstr = value.toString().trim();
      return vstr.length() > 0 ? vstr : null;
    }
    else {
      int s = str.indexOf(separator, pos);
      if(s > -1) {
        p.pos = s;
        return str.substring(pos, s).trim();
      }
      else {
        p.pos = str.length();
        return str.substring(pos).trim();
      }
      
    }
    
  }

  /**
   * @see de.stationadmin.base.track.format.TrackExportFormat#supports(java.lang.String)
   */
  @Override
  public boolean supports(String str) {
    if(str.length() > 0 && str.charAt(0) == '"') {
      return this.fromString(str) != null;
    }
    for(char c : separators) {
      if(str.indexOf(c) > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * @see de.stationadmin.base.track.format.TrackExportFormat#toString(de.stationadmin.base.track.BasicTrack)
   */
  @Override
  public String toString(BasicTrack title) {
    StringBuffer buf = new StringBuffer(100);
    buf.append(quote(title.getArtist()));
    buf.append(',');
    buf.append(quote(title.getTitle()));
    return buf.toString();
  }

  private String quote(String str) {
    str = StringUtils.replace(str, "\"", "\"\"");
    return "\"" + str + "\"";
  }
  
  private static class Position {
    int pos;
  }

}
