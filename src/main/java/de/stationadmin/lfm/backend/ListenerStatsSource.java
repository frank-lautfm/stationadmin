package de.stationadmin.lfm.backend;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "Source")
public class ListenerStatsSource {

  private int tlh;
  private int sessions;
  private int uniqs;

  public int getTlh() {
    return tlh;
  }

  public void setTlh(int tlh) {
    this.tlh = tlh;
  }

  public int getSessions() {
    return sessions;
  }

  public void setSessions(int sessions) {
    this.sessions = sessions;
  }

  public int getUniqs() {
    return uniqs;
  }

  public void setUniqs(int uniqs) {
    this.uniqs = uniqs;
  }

}
