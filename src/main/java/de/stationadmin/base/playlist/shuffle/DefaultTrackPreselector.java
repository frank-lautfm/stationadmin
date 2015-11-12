/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.stationadmin.base.track.Title;

/**
 * Random track preselection
 * 
 * @author korf
 */
public class DefaultTrackPreselector implements ArtistTrackPreselector {
  private Random random = new Random();

  @Override
  public List<Title> preselect(List<Title> tracks, int max) {
    if(tracks.size() <= max) {
      return tracks;
    }
    List<Title> preselected = new ArrayList<Title>(max);
   
    while(tracks.size() > 0 && preselected.size() < max) {
      int idx = this.random.nextInt(tracks.size());
      preselected.add(tracks.remove(idx));
    }
    
    return preselected;
  }

}
