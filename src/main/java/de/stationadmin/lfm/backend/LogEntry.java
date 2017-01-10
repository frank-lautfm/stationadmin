/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author korf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEntry {

  private String level;
  private String type;
  private int id;
  @JsonProperty("msg_type")
  private String msgType;
  private String message;
  private boolean shuffled;
  @JsonProperty("shuffle_time")
  private boolean shuffleTime;
  @JsonProperty("created_at")
  private Date createdAt;

  /**
   * @return the level
   */
  public String getLevel() {
    return level;
  }

  /**
   * @param level
   *          the level to set
   */
  public void setLevel(String level) {
    this.level = level;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type
   *          the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * @return the msgType
   */
  public String getMsgType() {
    return msgType;
  }

  /**
   * @param msgType
   *          the msgType to set
   */
  public void setMsgType(String msgType) {
    this.msgType = msgType;
  }

  /**
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @param message
   *          the message to set
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * @return the shuffled
   */
  public boolean isShuffled() {
    return shuffled;
  }

  /**
   * @param shuffled
   *          the shuffled to set
   */
  public void setShuffled(boolean shuffled) {
    this.shuffled = shuffled;
  }

  /**
   * @return the shuffleTime
   */
  public boolean isShuffleTime() {
    return shuffleTime;
  }

  /**
   * @param shuffleTime
   *          the shuffleTime to set
   */
  public void setShuffleTime(boolean shuffleTime) {
    this.shuffleTime = shuffleTime;
  }

  /**
   * @return the createdAt
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * @param createdAt
   *          the createdAt to set
   */
  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

}
