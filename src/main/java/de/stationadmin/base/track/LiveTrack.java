package de.stationadmin.base.track;

import org.apache.commons.lang3.StringUtils;

/**
 * Track that was played live
 */
public class LiveTrack extends BasicTrack {

  public LiveTrack() {

  }

  public LiveTrack(String str) {
    String[] parts = StringUtils.split(str, '\t');
    try {
      setId(Integer.parseInt(parts[0]));
      setArtist(parts[1]);
      setTitle(parts[2]);
    } catch (Exception e) {

    }
  }
  
  private static String removeTabs(String str) {
    return str.replace('\t', ' ');
  }

  public String toFullString() {
    return getId() + "\t" + removeTabs(getArtist()) + "\t" + removeTabs(getTitle());
  }
}
