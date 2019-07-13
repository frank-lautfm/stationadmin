/**
 * 
 */
package de.stationadmin.base.tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.BasicTrack;

/**
 * @author Frank
 * 
 */
public class DynamicTag implements Tag {
  private String filename;
  private String registeredName;
  private String name;
  private String group;
  private String artists;
  private String titles;
  private String albums;
  private boolean playedWithinInverse = false;
  private int playedWithin = 0;
  private int playedWithinMinHour = -1, playedWithinMaxHour = -1;
  private int playedWithinPlaylist = -1;
  private int[] playlistIds;
  private String[] tags;
  private int minLength = 0;
  private int maxLength = Integer.MAX_VALUE;
  private transient Matcher matcher;

  @Override
  public int compareTo(Tag o) {
    return this.getName().compareToIgnoreCase(o.getName());
  }

  public boolean contains(BasicTrack title) {
    if (this.matcher == null) {
      this.matcher = new Matcher();
    }
    return this.matcher.matches(title);
  }

  void delete() throws IOException {
    new File(this.filename).delete();
  }

  /**
   * @return the albums
   */
  public String getAlbums() {
    return albums;
  }

  /**
   * @return the artists
   */
  public String getArtists() {
    return artists;
  }

  /**
   * @return the filename
   */
  String getFilename() {
    return filename;
  }

  /**
   * @return the maxLength
   */
  public int getMaxLength() {
    return maxLength;
  }

  /**
   * @return the minLength
   */
  public int getMinLength() {
    return minLength;
  }

  @Override
  public String getName() {
    return this.name;
  }

  /**
   * @return the registeredName
   */
  String getRegisteredName() {
    return registeredName;
  }

  /**
   * @return the titles
   */
  public String getTitles() {
    return titles;
  }

  void load() throws IOException {
    List<String> lines;
    try (FileInputStream input = new FileInputStream(new File(this.filename))) {
      lines = IOUtils.readLines(input, "UTF-8");
    }
    load(lines);
  }

  private void load(List<String> lines) {
    for (String line : lines) {
      int eq = line.indexOf('=');
      if (eq > -1 && eq < line.length() - 2) {
        String key = line.substring(0, eq).trim();
        String value = line.substring(eq + 1).trim();
        if (key.equals("name")) {
          this.name = value;
        } else if (key.equals("group")) {
          this.group = value;
        } else if (key.equals("artists")) {
          this.artists = readString(value);
        } else if (key.equals("albums")) {
          this.albums = readString(value);
        } else if (key.equals("titles")) {
          this.titles = readString(value);
        } else if (key.equals("minLength")) {
          try {
            this.minLength = Integer.parseInt(value);
          } catch (NumberFormatException e) {
          }
        } else if (key.equals("maxLength")) {
          try {
            this.maxLength = Integer.parseInt(value);
          } catch (NumberFormatException e) {
          }
        } else if (key.equals("playedWithin")) {
          try {
            this.playedWithin = Integer.parseInt(value);
          } catch (NumberFormatException e) {
          }
        } else if (key.equals("playedWithinInverse")) {
          this.playedWithinInverse = Boolean.parseBoolean(value);
        } else if (key.equals("playedWithin.minHour")) {
          try {
            this.playedWithinMinHour = Integer.parseInt(value);
          } catch (NumberFormatException e) {
          }
        } else if (key.equals("playedWithin.maxHour")) {
          try {
            this.playedWithinMaxHour = Integer.parseInt(value);
          } catch (NumberFormatException e) {
          }
        } else if (key.equals("playedWithin.playlist")) {
          try {
            this.playedWithinPlaylist = Integer.parseInt(value);
          } catch (NumberFormatException e) {
          }
        } else if (key.equals("playlists")) {
          String[] idsAsStr = StringUtils.split(value, ",");
          int[] ids = new int[idsAsStr.length];
          for (int i = 0; i < idsAsStr.length; i++) {
            try {
              ids[i] = Integer.parseInt(idsAsStr[i]);
            } catch (NumberFormatException e) {
            }
          }
          this.playlistIds = ids;
        } else if (key.equals("tags")) {
          this.tags = StringUtils.split(value, "\t");
        }
      }
    }
  }

  private static String readString(String str) {
    return str.replace(';', '\n');
  }

  private static String writeString(String str) {
    StringBuilder buf = new StringBuilder(str.length());
    for (int i = 0; i < str.length(); i++) {
      if (str.charAt(i) == '\n') {
        buf.append(';');
      } else if (str.charAt(i) != '\r') {
        buf.append(str.charAt(i));
      }
    }
    return buf.toString();
  }
  
  public void setConfiguration(String content) {
    load(Arrays.asList(StringUtils.split(content, "\n")));
  }

  public String getConfiguration() {
    StringBuilder buf = new StringBuilder();
    buf.append("name = " + this.name + "\n");
    if (StringUtils.isNotEmpty(this.group)) {
      buf.append("group = " + group + "\n");
    }
    if (StringUtils.isNotEmpty(this.artists)) {
      buf.append("artists = " + writeString(this.artists) + "\n");
    }
    if (StringUtils.isNotEmpty(this.titles)) {
      buf.append("titles = " + writeString(this.titles) + "\n");
    }
    if (StringUtils.isNotEmpty(this.albums)) {
      buf.append("albums = " + writeString(this.albums) + "\n");
    }
    buf.append("minLength = " + this.minLength + "\n");
    buf.append("maxLength = " + this.maxLength + "\n");
    if (this.playedWithin > 0) {
      buf.append("playedWithin = " + this.playedWithin + "\n");
      buf.append("playedWithinInverse = " + Boolean.toString(this.playedWithinInverse) + "\n");
    }
    if (this.playedWithinMinHour > -1) {
      buf.append("playedWithin.minHour = " + this.playedWithinMinHour + "\n");
    }
    if (this.playedWithinMaxHour > -1) {
      buf.append("playedWithin.maxHour = " + this.playedWithinMaxHour + "\n");
    }
    if (this.playedWithinPlaylist > -1) {
      buf.append("playedWithin.playlist = " + this.playedWithinPlaylist + "\n");
    }
    if (this.playlistIds != null && this.playlistIds.length > 0) {
      buf.append("playlists = ");
      for (int i = 0; i < this.playlistIds.length; i++) {
        if (i > 0) {
          buf.append(',');
        }
        buf.append(this.playlistIds[i]);
      }
    }
    if (this.tags != null && this.tags.length > 0) {
      buf.append("tags = ");
      for (int i = 0; i < this.tags.length; i++) {
        if (i > 0) {
          buf.append('\t');
        }
        buf.append(this.tags[i]);
      }

    }
    return buf.toString();

  }

  void save() throws IOException {
    try (FileOutputStream out = new FileOutputStream(new File(this.filename))) {
      IOUtils.write(getConfiguration(), out, "UTF-8");
    }
  }

  /**
   * @param albums the albums to set
   */
  public void setAlbums(String albums) {
    this.albums = albums;
    this.matcher = null;
  }

  /**
   * @param artists the artists to set
   */
  public void setArtists(String artists) {
    this.artists = artists;
    this.matcher = null;
  }

  /**
   * @param filename the filename to set
   */
  void setFilename(String filename) {
    this.filename = filename;
  }

  /**
   * @param maxLength the maxLength to set
   */
  public void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }

  /**
   * @param minLength the minLength to set
   */
  public void setMinLength(int minLength) {
    this.minLength = minLength;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @param registeredName the registeredName to set
   */
  void setRegisteredName(String registeredName) {
    this.registeredName = registeredName;
  }

  /**
   * @param titles the titles to set
   */
  public void setTitles(String titles) {
    this.titles = titles;
    this.matcher = null;
  }

  private class Matcher {
    private String[] artistPatterns;
    private String[] titlePatterns;
    private String[] albumPatterns;

    Matcher() {
      this.artistPatterns = this.toPatterns(artists);
      this.titlePatterns = this.toPatterns(titles);
      this.albumPatterns = this.toPatterns(albums);
    }

    private boolean matches(String[] patterns, String str) {
      if (patterns == null) {
        return true;
      }
      if (str == null) {
        return false;
      }
      for (String pattern : patterns) {
        // wildcardMatch is written for sth different, but works great for our
        // purposes here
        if (FilenameUtils.wildcardMatch(str, pattern, IOCase.INSENSITIVE)) {
          return true;
        }
      }

      return false;

    }

    public boolean matches(BasicTrack title) {
      if (matches(this.artistPatterns, title.getArtist()) && matches(this.titlePatterns, title.getTitle()) && title.getLength() > minLength && title.getLength() < maxLength) {
        if (this.albumPatterns != null) {
          return (title instanceof RegisteredTrack) && matches(this.albumPatterns, ((RegisteredTrack) title).getAlbum());
        } else {
          return true;
        }
      }

      return false;
    }

    String[] toPatterns(String str) {
      if (str != null) {
        String[] arr = StringUtils.split(str, ";\n\r");
        for (int i = 0; i < arr.length; i++) {
          arr[i] = arr[i].trim().toLowerCase();
        }
        return arr;
      } else {
        return null;
      }
    }

  }

  @Override
  public String toString() {
    return this.name;
  }

  public int getPlayedWithin() {
    return playedWithin;
  }

  public void setPlayedWithin(int playedWithin) {
    this.playedWithin = playedWithin;
  }

  public int getPlayedWithinMinHour() {
    return playedWithinMinHour;
  }

  public void setPlayedWithinMinHour(int playedWithinMinHour) {
    this.playedWithinMinHour = playedWithinMinHour;
  }

  public int getPlayedWithinMaxHour() {
    return playedWithinMaxHour;
  }

  public void setPlayedWithinMaxHour(int playedWithinMaxHour) {
    this.playedWithinMaxHour = playedWithinMaxHour;
  }

  public int getPlayedWithinPlaylist() {
    return playedWithinPlaylist;
  }

  public void setPlayedWithinPlaylist(int playedWithinPlaylist) {
    this.playedWithinPlaylist = playedWithinPlaylist;
  }

  public int[] getPlaylistIds() {
    return playlistIds;
  }

  public void setPlaylistIds(int[] playlistIds) {
    this.playlistIds = playlistIds;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String[] getTags() {
    return tags;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

  public boolean isPlayedWithinInverse() {
    return playedWithinInverse;
  }

  public void setPlayedWithinInverse(boolean playedWithinInverse) {
    this.playedWithinInverse = playedWithinInverse;
  }

}
