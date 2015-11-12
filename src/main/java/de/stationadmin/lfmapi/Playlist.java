/**
 * 
 */
package de.stationadmin.lfmapi;

import java.io.Serializable;

import org.json.JSONObject;

/**
 * @author Frank
 *
 */
public class Playlist implements Serializable {
  private static final long serialVersionUID = -8033785276667742136L;

  private int id;
  private String name;
  private String color;
  private boolean shuffled;
  private int length;

  public Playlist(JSONObject json) {
    this.id = JSONUtil.getInt(json, "id", 0);
    this.name = JSONUtil.getString(json, "name");
    this.color = JSONUtil.getString(json, "color");
    this.shuffled = JSONUtil.getBoolean(json, "shuffled");
    this.length = JSONUtil.getInt(json, "length", 0);
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the color
   */
  public String getColor() {
    return color;
  }

  /**
   * @return the shuffled
   */
  public boolean isShuffled() {
    return shuffled;
  }

  /**
   * @return the id
   */
  public int getId() {
    return id;
  }

  public String toString() {
    return this.name;
  }

  /**
   * @return the length
   */
  public int getLength() {
    return length;
  }

}
