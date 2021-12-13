package de.stationadmin.base.playlist.shuffle;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.track.TrackService;

public class NewsEngine implements PlaylistEnhancer {

  private TrackService trackService;
  private Logger log = LogManager.getLogger(NewsEngine.class);
  private boolean shuffleMode;

  public NewsEngine(TrackService trackService, boolean shuffleMode) {
    this.trackService = trackService;
    this.shuffleMode = shuffleMode;
  }

  @Override
  public boolean excludeFromCorePlaylist(BasicTrack track) {
    return track.getId() == TrackRegistry.LAUTFM_NEWS_ID;
  }

  private boolean isApplicable(Playlist playlist) {
    if (shuffleMode) {
      for (Entry entry : playlist.getEntries()) {
        if (entry.getTrackId() == TrackRegistry.LAUTFM_NEWS_ID) {
          return true;
        }
      }
    } else {
      return playlist.getGenerateNewsInterval() > 0;
    }
    return false;
  }

  @Override
  public List<BasicTrack> process(Playlist playlist, List<BasicTrack> tracks, boolean protectFirstJingle) {
    if (!isApplicable(playlist)) {
      return tracks;
    }
    int interval = shuffleMode ? 60 * 60 : playlist.getGenerateNewsInterval() * 60;
    boolean firstJingleAfterNews = (shuffleMode ? protectFirstJingle : playlist.getGenerateFirstJingleAfterNews()) && tracks.get(0).getType() == BasicTrack.TYPE_JINGLE;
    int newsLength = shuffleMode ? 150 : 160;
    try {
      BasicTrack newsTrack = trackService.getTrack(TrackRegistry.LAUTFM_NEWS_ID);

      int totalDuration = 0;
      for (int i = 0; i < tracks.size(); i++) { 
        totalDuration += tracks.get(i).getLength();
      }

      
      List<BasicTrack> newList = new ArrayList<>();
      newList.add(newsTrack);
      int nextNewsInsert = interval;
      int time = newsLength;
      int tolerance = shuffleMode ? 0 : 2;
      for (int i = 0; i < tracks.size(); i++) {
        BasicTrack track = tracks.get(i);
        if (time > nextNewsInsert - tolerance * 60 && (track.getType() == BasicTrack.TYPE_MUSIC || track.getLength() > 90) && time < totalDuration - 60 * 15) {
          newList.add(newsTrack);
          time += newsLength;
          nextNewsInsert += interval;
          if (firstJingleAfterNews) {
            newList.add(tracks.get(0));
            time += tracks.get(0).getLength();
          }
        }
        time += track.getLength();
        newList.add(track);
      }

      return newList;
    } catch (Exception e) {
      log.error("failed to add news", e);
      return tracks;
    }
  }

  @Override
  public void reset() {

  }

  @Override
  public void initialize(PlaylistProfile profile) {
  }

}
