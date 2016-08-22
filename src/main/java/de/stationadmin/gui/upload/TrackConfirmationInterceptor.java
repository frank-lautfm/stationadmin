/**
 * 
 */
package de.stationadmin.gui.upload;

import java.util.List;

import de.stationadmin.base.track.DetailedTrack;

/**
 * @author Frank
 *
 */
public interface TrackConfirmationInterceptor {
  
  void beforeDisplay(List<DetailedTrack> titles);
  
  void beforeSave(List<DetailedTrack> titles);
  
  void afterSave(List<DetailedTrack> titles);

}
