/**
 * 
 */
package de.stationadmin.base.track;

/**
 * A TrackAttachment is a piece of information that is attached to a {@link RegisteredTrack}
 * and which handles one aspect of the track.
 */
public interface TrackAttachment {
  
  /**
   * Checks if the track is used
   * @return
   */
  boolean isUsed();

}
