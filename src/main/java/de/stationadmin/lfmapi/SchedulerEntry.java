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
public class SchedulerEntry implements Serializable {
  private static final long serialVersionUID = 8673041511810893039L;

  private int id;
  private String name;
  private String description;
  private int length;
  private String color;
  private boolean shuffled;

  private AirTime airTime;

  public SchedulerEntry(JSONObject json) {
    this.id = JSONUtil.getInt(json, "id", 0);
    this.name = JSONUtil.getString(json, "name");
    this.color = JSONUtil.getString(json, "color");
    this.shuffled = JSONUtil.getBoolean(json, "shuffled");
    this.description = JSONUtil.getString(json, "description");
    this.length = JSONUtil.getInt(json, "length", 0);
    this.airTime = new AirTime(json);
  }

  /**
   * @return the serialversionuid
   */
  public static long getSerialversionuid() {
    return serialVersionUID;
  }

  /**
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return the length
   */
  public int getLength() {
    return length;
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
   * @return the day
   */
  public String getDay() {
    return this.airTime.getDay();
  }

  /**
   * @return the hour
   */
  public int getHour() {
    return this.airTime.getHour();
  }

  /**
   * @return the endTime
   */
  public int getEndTime() {
    return this.airTime.getEndTime();
  }
  
  public String toString() {
    return this.airTime.getDay() + ", " + this.airTime.getHour() + "-" + this.airTime.getEndTime() + ": " + this.name;
  }

  /**
   * @return the airTime
   */
  public AirTime getAirTime() {
    return airTime;
  }

}
