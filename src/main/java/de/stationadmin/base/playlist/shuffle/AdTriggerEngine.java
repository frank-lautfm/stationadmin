package de.stationadmin.base.playlist.shuffle;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.util.TimeFormat;

public class AdTriggerEngine implements PlaylistEnhancer {
  private Logger log = Logger.getLogger(AdTriggerEngine.class);
  
  public enum AdJingleCollisionStrategy {
    KEEP_BOTH, REMOVE_JINGLE, MOVE_ADTRIGGER
  }

  private TrackRegistry trackRegistry;
  private int position1 = 0;
  private int position2 = 30;
  private int adSeparatorId = -1;
  private int adTriggerId = TrackRegistry.STANDARD_AD_TRIGGER_ID;
  private AdJingleCollisionStrategy jingleCollisionStrategy = AdJingleCollisionStrategy.KEEP_BOTH;
  
  private boolean clearExistingTriggers;

  public AdTriggerEngine(TrackRegistry trackRegistry) {
    this.trackRegistry = trackRegistry;
  }

  @Override
  public boolean excludeFromCorePlaylist(BasicTrack track) {
    return track.getId() == adSeparatorId || track.getId() == adTriggerId || track.getId() == TrackRegistry.STANDARD_AD_TRIGGER_ID;
  }

  public int getAdSeparatorId() {
    return adSeparatorId;
  }

  public int getAdTriggerId() {
    return adTriggerId;
  }

  public AdJingleCollisionStrategy getJingleCollisionStrategy() {
    return jingleCollisionStrategy;
  }

  public int getPosition1() {
    return position1;
  }

  public int getPosition2() {
    return position2;
  }

  public TrackRegistry getTrackRegistry() {
    return trackRegistry;
  }

  @Override
  public List<BasicTrack> process(Playlist playlist, List<BasicTrack> tracks, boolean protectFirstJingle) {
    List<BasicTrack> newTracks = new ArrayList<BasicTrack>();

    BasicTrack adSeparator = this.adSeparatorId > 0 ? this.trackRegistry.getTrack(this.adSeparatorId) : null;
    BasicTrack adTrigger = this.trackRegistry.getTrack(this.adTriggerId);
    if (adTrigger == null) {
      // use default ad trigger
      adTrigger = this.trackRegistry.getStandardAdTrigger();
    }

    if (position1 > position2) {
      log.debug("swap positions");
      int tmp = position1;
      position1 = position2;
      position2 = tmp;
    }

    int diff = position2 - position1;
    if (diff < 20 && diff > 40) {
      if (position1 > 30) {
        position1 = 30;
      }
      if (diff < 20) {
        position2 = position1 + 20;
      } else if (diff > 40) {
        position2 = position1 + 40;
      }
      log.warn("Configured ad positions too close to each other - adjusting to " + position1 + " / " + position2);
    }

    int currentPosition = 0;
    int adCnt = 0;
    int nextAdPosition = this.position1 * 60;

    boolean allowMove = true;
    for (int i = 0; i < tracks.size(); i++) {
      if(clearExistingTriggers) {
        if(tracks.get(i).getId() == adTriggerId || tracks.get(i).getId() == 0 || tracks.get(i).getId() == adSeparatorId) {
          continue;
        }
      }
      
      boolean addTrigger = currentPosition > nextAdPosition;
      boolean addTrack = true;

      if (addTrigger && jingleCollisionStrategy != AdJingleCollisionStrategy.KEEP_BOTH) {
        boolean lastIsJingle = newTracks.size() > 0 && newTracks.get(newTracks.size() - 1).getType() == BasicTrack.TYPE_JINGLE;
        boolean nextIsJingle = tracks.get(i).getType() == BasicTrack.TYPE_JINGLE;
        if (lastIsJingle || nextIsJingle) {
          if (jingleCollisionStrategy == AdJingleCollisionStrategy.MOVE_ADTRIGGER) {
            if (allowMove) {
              // prevent adding trigger for now and move next position 60 seconds ahead
              addTrigger = false;
              nextAdPosition += 60;
              allowMove = false;
            }
          } else if (jingleCollisionStrategy == AdJingleCollisionStrategy.REMOVE_JINGLE) {
            if (lastIsJingle) {
              newTracks.remove(newTracks.size() - 1);
            }
            if (nextIsJingle) {
              addTrack = false; // block adding of next track (which is a jingle)
            }
          }
        }
      }

      if (addTrigger) {
        log.info("Placing ad trigger at " + TimeFormat.format(currentPosition, true) + " within " + playlist.getName());
        currentPosition += addAdTrigger(newTracks, adSeparator, adTrigger);
        adCnt++;
        int nextAdBase = adCnt % 2 == 0 ? this.position1 : this.position2;
        nextAdPosition = (nextAdBase * 60) + (adCnt / 2) * 60 * 60;
        // System.out.println("next ad position " + TimeFormat.format(nextAdPosition / 60, true) + " / " + nextAdBase + " / " + adCnt + " / " + (adCnt / 2));
        allowMove = true;
      }
      if (addTrack) {
        newTracks.add(tracks.get(i));
        currentPosition += tracks.get(i).getLength();
      }
    }

    return newTracks;
  }

  private int addAdTrigger(List<BasicTrack> tracks, BasicTrack adSeparator, BasicTrack adTrigger) {
    int timeAdded = 0;
    if (adSeparator != null) {
      timeAdded += adSeparator.getLength();
      tracks.add(adSeparator);
    }
    timeAdded += adTrigger.getLength();
    tracks.add(adTrigger);
    return timeAdded;
  }

  @Override
  public void reset() {
  }

  public void setAdSeparatorId(int adSeparatorId) {
    this.adSeparatorId = adSeparatorId;
  }

  public void setAdTriggerId(int adTriggerId) {
    this.adTriggerId = adTriggerId;
  }

  public void setJingleCollisionStrategy(AdJingleCollisionStrategy jingleCollisionStrategy) {
    this.jingleCollisionStrategy = jingleCollisionStrategy;
  }

  public void setPosition1(int position1) {
    this.position1 = position1;
  }

  public void setPosition2(int position2) {
    this.position2 = position2;
  }

  public void setTrackRegistry(TrackRegistry trackRegistry) {
    this.trackRegistry = trackRegistry;
  }

  public boolean isClearExistingTriggers() {
    return clearExistingTriggers;
  }

  public void setClearExistingTriggers(boolean clearExistingTriggers) {
    this.clearExistingTriggers = clearExistingTriggers;
  }
}
