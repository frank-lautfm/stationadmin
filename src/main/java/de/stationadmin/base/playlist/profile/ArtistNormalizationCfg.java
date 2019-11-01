package de.stationadmin.base.playlist.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "ArtistNormalizationCfg")
public class ArtistNormalizationCfg {

  private List<String> separators;
  private Map<String, String> aliases;

  public ArtistNormalizationCfg() {

  }

  public ArtistNormalizationCfg(ArtistNormalizationCfg source) {
    if (source.separators != null) {
      this.separators = new ArrayList<>(source.separators);
    }
    if (source.aliases != null) {
      this.aliases = new HashMap<>(source.aliases);
    }
  }

  public List<String> getSeparators() {
    return separators;
  }

  public void setSeparators(List<String> separators) {
    this.separators = separators;
  }

  public Map<String, String> getAliases() {
    return aliases;
  }

  public void setAliases(Map<String, String> aliases) {
    this.aliases = aliases;
  }

}
