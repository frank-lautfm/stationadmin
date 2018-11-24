package de.stationadmin.lfm.backend;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "XPlaylistHead")
public class ExtendedPlaylistHead extends PlaylistHead {
  
  @JsonProperty("shuffle_function")
  private String shuffleFunction;

  public String getShuffleFunction() {
    return shuffleFunction;
  }

  public void setShuffleFunction(String shuffleFunction) {
    this.shuffleFunction = shuffleFunction;
  }


}
