/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

/**
 * Strategy for distributing word track when shuffling or generating playlists
 * 
 * @author Frank
 */
public enum WordDistributionStrategy {
  RANDOM,
  PROTECT,
  SUCCESSOR_COUPLING,
  PREDECESSOR_COUPLING
}
