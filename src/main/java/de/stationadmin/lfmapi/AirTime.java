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
public class AirTime implements Serializable {
  private static final long serialVersionUID = -1524112980395776399L;
  private String day;
  private int hour;
  private int endTime;
  
  public AirTime(JSONObject json) {
    this.day = JSONUtil.getString(json, "day");
    this.hour = JSONUtil.getInt(json, "hour", 0);
    this.endTime = JSONUtil.getInt(json, "end_time", 0);

  }

  /**
   * @param day
   * @param hour
   * @param endTime
   */
  public AirTime(String day, int hour, int endTime) {
    super();
    this.day = day;
    this.hour = hour;
    this.endTime = endTime;
  }

  /**
   * @return the day
   */
  public String getDay() {
    return day;
  }

  /**
   * @return the hour
   */
  public int getHour() {
    return hour;
  }

  /**
   * @return the endTime
   */
  public int getEndTime() {
    return endTime;
  }

}
