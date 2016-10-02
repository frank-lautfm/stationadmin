/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.stationadmin.base.track.BasicTrack;

/**
 * Random track preselection
 * 
 * @author korf
 */
public class DefaultTrackPreselector implements ArtistTrackPreselector {
  private Random random = new Random();

  @Override
  public List<BasicTrack> preselect(List<BasicTrack> tracks, int max) {
    if(tracks.size() <= max) {
      return tracks;
    }
    List<BasicTrack> preselected = new ArrayList<BasicTrack>(max);
   
    while(tracks.size() > 0 && preselected.size() < max) {
      int idx = this.random.nextInt(tracks.size());
      preselected.add(tracks.remove(idx));
    }
    
    return preselected;
  }

}
