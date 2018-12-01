package de.stationadmin.lfm.backend;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "CurrentPlaylistInfo")
public class CurrentPlaylistInfo {

  private int id;
  private String title;
  @JsonProperty("shuffle_time")
  private double shuffleTime;
  private boolean shuffled;
  private String reason;
  @JsonProperty("shuffle_function")
  private String shuffleFunction;
  @JsonProperty("shuffle_opts")
  private Map<String, Object> shuffleOpts;
  
  public CurrentPlaylistInfo() {
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public double getShuffleTime() {
    return shuffleTime;
  }

  public void setShuffleTime(double shuffleTime) {
    this.shuffleTime = shuffleTime;
  }

  public boolean isShuffled() {
    return shuffled;
  }

  public void setShuffled(boolean shuffled) {
    this.shuffled = shuffled;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getShuffleFunction() {
    return shuffleFunction;
  }

  public void setShuffleFunction(String shuffleFunction) {
    this.shuffleFunction = shuffleFunction;
  }

  public Map<String, Object> getShuffleOpts() {
    return shuffleOpts;
  }

  public void setShuffleOpts(Map<String, Object> shuffleOpts) {
    this.shuffleOpts = shuffleOpts;
  }

}
