/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

/**
 * Normalizes an artist string, for example by removing a "feat. by xyz".
 * 
 * @author Frank
 */
public interface ArtistNormalizer {

  /**
   * Normalizes the given artist string
   * @param artist
   * @return
   */
  String normalizeArtist(String artist);
}
