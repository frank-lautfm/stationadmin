package de.stationadmin.lfm.backend;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "QueueStatus")
public class QueueStatus {
	
  @JsonProperty("queue_status")
	private String status;
	
  @JsonProperty("queue_user")
	private int queueUser;

  @JsonProperty("queue_station")
	private int queueStation;

  @JsonProperty("user_limit")
	private int userLimit;

  @JsonProperty("station_limit")
	private int stationLimit;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getQueueUser() {
		return queueUser;
	}

	public void setQueueUser(int queueUser) {
		this.queueUser = queueUser;
	}

	public int getQueueStation() {
		return queueStation;
	}

	public void setQueueStation(int queueStation) {
		this.queueStation = queueStation;
	}

	public int getUserLimit() {
		return userLimit;
	}

	public void setUserLimit(int userLimit) {
		this.userLimit = userLimit;
	}

	public int getStationLimit() {
		return stationLimit;
	}

	public void setStationLimit(int stationLimit) {
		this.stationLimit = stationLimit;
	}
	
	public int getEffectiveLimit() {
		return Math.min(userLimit, stationLimit);
	}
	
	public boolean IsOverloaded() {
		return this.status != null && this.status.equals("overloaded");
	}

	
}
