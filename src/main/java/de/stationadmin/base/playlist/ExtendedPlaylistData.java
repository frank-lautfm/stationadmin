/**
 * 
 */
package de.stationadmin.base.playlist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author Frank Korf
 * 
 */
public class ExtendedPlaylistData {
  private static final int VERSION = 1;
  private int id;
  private boolean shuffleAllowed = true;
  private boolean gvlCheck = true;
  private Set<String> tags = new HashSet<String>();
  private int generateLength = 2;
  private String generateTags;
  private boolean generateTagsAll = false;
  private String generatePushTag;
  private boolean generateMinimizeArtistRepeats = true;
  private int generateTitleRepeatLevel = -1;
  private String[] generateAdvices = null;
  private int generateMaxArtistTitles = 3;
  private String comment;

  public ExtendedPlaylistData(int id) {
    super();
    this.id = id;
  }

  /**
   * @return the shuffleAllowed
   */
  public boolean isShuffleAllowed() {
    return shuffleAllowed;
  }

  /**
   * @param shuffleAllowed
   *          the shuffleAllowed to set
   */
  public void setShuffleAllowed(boolean shuffleAllowed) {
    this.shuffleAllowed = shuffleAllowed;
  }

  /**
   * @return the tags
   */
  public Set<String> getTags() {
    return tags;
  }

  /**
   * @param tags
   *          the tags to set
   */
  public void setTags(Set<String> tags) {
    this.tags = tags;
  }

  /**
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * @return the generateLength
   */
  public int getGenerateLength() {
    return generateLength;
  }

  /**
   * @param generateLength
   *          the generateLength to set
   */
  public void setGenerateLength(int generateLength) {
    this.generateLength = generateLength;
  }

  /**
   * @return the generateTags
   */
  public String getGenerateTags() {
    return generateTags;
  }

  /**
   * @param generateTags
   *          the generateTags to set
   */
  public void setGenerateTags(String generateTags) {
    this.generateTags = generateTags;
  }

  /**
   * @return the generatePushTag
   */
  public String getGeneratePushTag() {
    return generatePushTag;
  }

  /**
   * @param generatePushTag
   *          the generatePushTag to set
   */
  public void setGeneratePushTag(String generatePushTag) {
    this.generatePushTag = generatePushTag;
  }

  /**
   * @return the generateTagsAll
   */
  public boolean isGenerateTagsAll() {
    return generateTagsAll;
  }

  /**
   * @param generateTagsAll
   *          the generateTagsAll to set
   */
  public void setGenerateTagsAll(boolean generateTagsAll) {
    this.generateTagsAll = generateTagsAll;
  }

  /**
   * Exports the data a list of lines with key-value pairs
   * 
   * @return
   */
  public List<String> export() {
    List<String> properties = new ArrayList<String>();
    properties.add("version = " + VERSION);
    properties.add("id = " + this.id);
    properties.add("shuffleAllowed = " + Boolean.toString(this.shuffleAllowed));
    properties.add("gvlCheck = " + Boolean.toString(this.gvlCheck));
    int tagIdx = 1;
    for (String tag : this.tags) {
      properties.add("tag." + tagIdx + " = " + tag);
      tagIdx++;
    }
    if (StringUtils.isNotEmpty(this.generateTags)) {
      properties.add("generate.tags = " + this.generateTags);
      if (this.generatePushTag != null) {
        properties.add("generate.tags.push = " + this.generatePushTag);
      }
      properties.add("generate.tags.all = " + Boolean.toString(this.generateTagsAll));
      properties.add("generate.length = " + Integer.toString(this.generateLength));
      properties.add("generate.minimizeArtistRepeats = " + Boolean.toString(this.generateMinimizeArtistRepeats));
      properties.add("generate.titleRepeatLevel = " + Integer.toString(this.generateTitleRepeatLevel));
      properties.add("generate.maxArtistTitles= " + Integer.toString(this.generateMaxArtistTitles));

      if (this.generateAdvices != null) {
        for (int i = 0; i < this.generateAdvices.length; i++) {
          properties.add("generate.advice." + i + " = " + this.generateAdvices[i]);
        }
      }

    }
    if (StringUtils.isNotEmpty(this.comment)) {
      properties.add("comment = " + this.comment.replaceAll("[\\r|\\n]+", " "));
    }
    return properties;
  }

  protected static ExtendedPlaylistData create(Map<String, String> map) {
    int id = map.containsKey("id") ? Integer.parseInt(map.get("id")) : -1;
    if (id > -1) {
      ExtendedPlaylistData data = new ExtendedPlaylistData(id);
      data.setShuffleAllowed(map.containsKey("shuffleAllowed") ? map.get("shuffleAllowed").equalsIgnoreCase("true") : false);

      data.setGvlCheck(map.containsKey("gvlCheck") ? map.get("gvlCheck").equalsIgnoreCase("true") : true);

      HashSet<String> tags = new HashSet<String>();
      int tagIdx = 1;
      while (map.containsKey("tag." + tagIdx)) {
        tags.add(map.get("tag." + tagIdx));
        tagIdx++;
      }
      data.setTags(tags);

      if (map.containsKey("generate.tags")) {
        data.setGenerateTags(map.get("generate.tags"));
        data.setGeneratePushTag(map.get("generate.tags.push"));
        if (data.getGeneratePushTag() != null && data.getGeneratePushTag().equals("null")) {
          data.setGeneratePushTag(null); // fix illegal entry from previous
                                         // version
        }
        if (map.containsKey("generate.tags.all")) {
          data.setGenerateTagsAll(map.get("generate.tags.all").equalsIgnoreCase("true"));
        }
        if (map.containsKey("generate.length")) {
          try {
            data.setGenerateLength(Integer.parseInt(map.get("generate.length")));
          } catch (NumberFormatException e) {
          }
        }
        if (map.containsKey("generate.minimizeArtistRepeats")) {
          data.setGenerateMinimizeArtistRepeats(map.get("generate.minimizeArtistRepeats").equalsIgnoreCase("true"));
        }
        if (map.containsKey("generate.titleRepeatLevel")) {
          data.setGenerateTitleRepeatLevel(Integer.parseInt(map.get("generate.titleRepeatLevel")));
        }
        if (map.containsKey("generate.maxArtistTitles")) {
          data.setGenerateMaxArtistTitles(Integer.parseInt(map.get("generate.maxArtistTitles")));
        }

        int adviceIdx = 0;
        List<String> advices = new ArrayList<String>();
        while (map.containsKey("generate.advice." + adviceIdx)) {
          advices.add(map.get("generate.advice." + adviceIdx));
          adviceIdx++;
        }
        if (advices.size() > 0) {
          data.setGenerateAdvices(advices.toArray(new String[advices.size()]));
        }

      }

      data.setComment(map.get("comment"));

      return data;
    } else {
      return null;
    }
  }

  /**
   * @return the generateMinimizeArtistRepeats
   */
  public boolean isGenerateMinimizeArtistRepeats() {
    return generateMinimizeArtistRepeats;
  }

  /**
   * @param generateMinimizeArtistRepeats
   *          the generateMinimizeArtistRepeats to set
   */
  public void setGenerateMinimizeArtistRepeats(boolean generateMinimizeArtistRepeats) {
    this.generateMinimizeArtistRepeats = generateMinimizeArtistRepeats;
  }

  /**
   * @return the generateTitleRepeatLevel
   */
  public int getGenerateTitleRepeatLevel() {
    return generateTitleRepeatLevel;
  }

  /**
   * @param generateTitleRepeatLevel
   *          the generateTitleRepeatLevel to set
   */
  public void setGenerateTitleRepeatLevel(int generateTitleRepeatLevel) {
    this.generateTitleRepeatLevel = generateTitleRepeatLevel;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String[] getGenerateAdvices() {
    return generateAdvices;
  }

  public void setGenerateAdvices(String[] generateAdvices) {
    this.generateAdvices = generateAdvices;
  }

  public int getGenerateMaxArtistTitles() {
    return generateMaxArtistTitles;
  }

  public void setGenerateMaxArtistTitles(int generateMaxArtistTitles) {
    this.generateMaxArtistTitles = generateMaxArtistTitles;
  }

  public boolean isGvlCheck() {
    return gvlCheck;
  }

  public void setGvlCheck(boolean gvlCheck) {
    this.gvlCheck = gvlCheck;
  }

  /**
   * @param id the id to set
   */
  void setId(int id) {
    this.id = id;
  }

}
