/**
 * 
 */
package de.stationadmin.base.track;


/**
 * Interface for classes that collect played titles
 * 
 * @author Frank Korf
 * @see PlaylistRecorder
 */
public interface TrackCollector {

  /**
   * Adds a title
   * @param title
   */
	void add(BasicTrack title);
}
