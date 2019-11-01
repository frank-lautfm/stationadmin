package de.stationadmin.base.playlist.profile;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "GenerateCfg")
public class GenerateCfg {

  private int minRandomValue;
  private Map<String,Integer> artistPreselectLimits;
  private Map<String,Integer> artistPreselectWeights;
  
  public GenerateCfg() {
    
  }
  
  public GenerateCfg(GenerateCfg source) {
    this.minRandomValue = source.getMinRandomValue();
    if(source.artistPreselectLimits != null) {
      artistPreselectLimits = new HashMap<>(source.artistPreselectLimits);
    }
    if(source.artistPreselectWeights != null) {
      artistPreselectWeights = new HashMap<>(source.artistPreselectWeights);
    }
  }
  
  public int getMinRandomValue() {
    return minRandomValue;
  }

  public void setMinRandomValue(int minRandomValue) {
    this.minRandomValue = minRandomValue;
  }

  public Map<String, Integer> getArtistPreselectLimits() {
    return artistPreselectLimits;
  }

  public void setArtistPreselectLimits(Map<String, Integer> artistPreselectLimits) {
    this.artistPreselectLimits = artistPreselectLimits;
  }

  public Map<String, Integer> getArtistPreselectWeights() {
    return artistPreselectWeights;
  }

  public void setArtistPreselectWeights(Map<String, Integer> artistPreselectWeights) {
    this.artistPreselectWeights = artistPreselectWeights;
  }

}
