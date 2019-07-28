package de.stationadmin.base.track;

import java.util.HashMap;
import java.util.Map;


/**
 * Basic track information
 * 
 * @author korf
 */
public class BasicTrack {
  private static Map<Character, Character> characterMapping = new HashMap<Character, Character>();
  
  public static final int TYPE_MUSIC = 1;
  public static final int TYPE_JINGLE = 2;
  public static final int TYPE_WORD = 3;
  public static final int TYPE_NEWS = 4;

  static {
    characterMapping.put('é', 'e');
    characterMapping.put('è', 'e');
    characterMapping.put('ê', 'e');
    characterMapping.put('ë', 'e');

    characterMapping.put('à', 'a');
    characterMapping.put('á', 'a');
    characterMapping.put('â', 'a');
    characterMapping.put('å', 'a');

    characterMapping.put('í', 'i');

    characterMapping.put('ä', 'a');
    characterMapping.put('ö', 'o');
    characterMapping.put('ü', 'u');

    characterMapping.put('ñ', 'n');
  }

  private int id;
  private String artist;
  private String title;
  private int length;
  private int type = 1;

  public BasicTrack() {
  }

  public BasicTrack(BasicTrack title) {
    this.id = title.id;
    this.artist = title.artist;
    this.title = title.title;
    this.length = title.length;
    this.type = title.type;
  }
  
  public BasicTrack(de.stationadmin.lfm.backend.Track track) {
    this.id = track.getId();
    this.update(track);
  }
  
  public void update(de.stationadmin.lfm.backend.Track track) {
    this.artist = track.getArtist();
    this.title = track.getTitle();
    this.length = track.getDuration();
    this.type = track.getType().equalsIgnoreCase("song") ? TYPE_MUSIC : track.getType().equalsIgnoreCase("jingle") ? TYPE_JINGLE : track.getType().equalsIgnoreCase("news") ? TYPE_NEWS : TYPE_WORD;
  }

  /**
   * Compares two artists. Takes care on some special characters and treats "&"
   * and "and" as equal term.
   * 
   * @param artist1
   * @param artist2
   * @return
   */
  public static boolean isArtistEqual(String artist1, String artist2) {
    if (artist1 != null) {
      artist1 = artist1.replaceAll("\\&", "and").trim();
      if(artist1.toLowerCase().startsWith("the ")) {
        artist1 = artist1.substring(4);
      }
    }
    if (artist2 != null) {
      artist2 = artist2.replaceAll("\\&", "and").trim();
      if(artist2.toLowerCase().startsWith("the ")) {
        artist2 = artist2.substring(4);
      }
    }
    return equals(artist1, artist2);
  }

  public static boolean equals(String str1, String str2) {
    if (str1 == null && str2 == null) {
      return true;
    }
    if ((str1 == null && str2 != null) || (str2 != null && str2 == null)) {
      return false;
    }
    str1 = str1.trim();
    str2 = str2.trim();
    if (str1.length() != str2.length()) {
      return false;
    }
    int len = str1.length();
    for (int i = 0; i < len; i++) {
      char c1 = Character.toLowerCase(str1.charAt(i));
      char c2 = Character.toLowerCase(str2.charAt(i));

      if (c1 != c2) {
        if (getMappedCharacter(c1) != getMappedCharacter(c2)) {
          return false;
        }
      }
    }

    return true;
  }

  private static char getMappedCharacter(char c) {
    Character mapped = characterMapping.get(c);
    return mapped != null ? mapped.charValue() : c;
  }

  /**
   * Checks if this title matches the given artist and title name
   * 
   * @param artist
   * @param title
   * @return
   */
  public boolean matches(String artist, String title) {
    boolean ok = isArtistEqual(this.artist, artist)
        && equals(this.title, title);
    return ok;
  }

  /**
   * Gets the database id of this title
   * 
   * @return
   */
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  /**
   * Gets the name of the artist
   * 
   * @return
   */
  public String getArtist() {
    return artist;
  }

  public void setArtist(String artist) {
    this.artist = artist;
  }

  /**
   * Gets the name of the title
   * 
   * @return
   */
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Gets the length in seconds
   * 
   * @return
   */
  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  @Override
  public String toString() {
    return this.artist + " - " + this.title;
  }

  public String toTabSeparatedValues() {
    StringBuffer buf = new StringBuffer();
    buf.append(this.id);
    buf.append('\t');
    buf.append(this.artist);
    buf.append('\t');
    buf.append(this.title);
    buf.append('\t');
    buf.append(this.length);
    buf.append('\t');
    buf.append(this.type);
    buf.append('\t');

    return buf.toString();

  }

  /**
   * @return the type
   */
  public int getType() {
    return type;
  }

  /**
   * @param type
   *          the type to set
   */
  public void setType(int type) {
    this.type = type;
  }

}
