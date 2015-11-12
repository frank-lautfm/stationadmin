/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default artist normalizer - transforms string in lowercase character and
 * removes everyting beyond a " feat".
 * 
 * @author Frank
 */
public class DefaultArtistNormalizer implements ArtistNormalizer {
  private String[] separators = {" feat"};
  private Map<String, String> aliases = new HashMap<String, String>();

  public DefaultArtistNormalizer() {
  }

  /**
   * @param separators
   */
  public DefaultArtistNormalizer(String... separators) {
    super();
    this.separators = separators;
  }
  
  public DefaultArtistNormalizer(List<String> separators) {
    super();
    this.separators = separators.toArray(new String[separators.size()]);
  }

  /**
   * Adds an artist alias
   * @param alias
   * @param artist
   */
  public void addAlias(String alias, String artist) {
    this.aliases.put(alias.toLowerCase(), artist.toLowerCase());
  }
  
  /**
   * @see de.stationadmin.base.playlist.shuffle.ArtistNormalizer#normalizeArtist(java.lang.String)
   */
  @Override
  public String normalizeArtist(String artist) {
    artist = artist.toLowerCase();
    if(this.aliases.containsKey(artist)) {
      artist = this.aliases.get(artist);
    }
    for (String separator : this.separators) {
      int pos = artist.indexOf(separator);
      if (pos > 0) {
        artist = artist.substring(0, pos);
        return artist;
      }
    }
    return artist;
  }

  /**
   * @return the separators
   */
  public String[] getSeparators() {
    return separators;
  }

}
