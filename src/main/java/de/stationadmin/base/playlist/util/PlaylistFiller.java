package de.stationadmin.base.playlist.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stationadmin.base.playlist.NewsTrackOption;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.playlist.shuffle.TrackRule;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackComparator;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.track.TrackService;

/**
 * Tool class for populating playlists based on autofill rules
 * 
 * @author fkorf
 */
public class PlaylistFiller {
  private PlaylistService playlistService;
  private TrackService trackService;
  private TrackRegistry trackRegistry;
  private TagManager tagManager;

  public PlaylistFiller(PlaylistService playlistService, TrackService trackService, TagManager tagManager) {
    super();
    this.playlistService = playlistService;
    this.trackService = trackService;
    this.trackRegistry = trackService.getTrackRegistry();
    this.tagManager = tagManager;
  }
  
  private static boolean isNewsOrJingle(BasicTrack track) {
  	return track.getType() == BasicTrack.TYPE_JINGLE || track.getType() == BasicTrack.TYPE_NEWS;
  }

  /**
   * Fills a single playlist with tracks based on autofill rules (if enabled)
   * 
   * @param playlist playlist to fill
   * @throws IOException
   */
  public void fillPlaylist(Playlist playlist) throws IOException, MissingSourceTracksException {
    if (playlist.getAutoFillRule().isEnabled()) {

    	List<BasicTrack> startTracks = new ArrayList<BasicTrack>();
      boolean weatherWasAtBeginning = false;
    	for(int i = 0; i < playlist.getEntries().size() && isNewsOrJingle(playlist.getEntry(i).getTrack()) ; i++) {
    		startTracks.add(playlist.getEntry(i).getTrack());
    		if(playlist.getEntry(i).getTrack().getId() == TrackRegistry.LAUTFM_NEWS_WEATHER_ID) {
    			weatherWasAtBeginning = true;
    		}
    	}
    	

      // --- Build track pool from sources ---
      Map<Integer, BasicTrack> tracks = new HashMap<>();

      // Source: Tags
      String[] tags = playlist.getAutoFillRule().getSourceTags();
      if (tags != null) {
        for (String tag : tags) {
          for (int trackId : tagManager.getTrackIds(tag)) {
            if (!tracks.containsKey(trackId)) {
              RegisteredTrack track = trackRegistry.getTrack(trackId);
              if (track != null) {
                tracks.put(trackId, track);
              }
            }
          }
        }
      }

      ArrayList<BasicTrack> trackList = new ArrayList<>(tracks.values());
      trackList.sort(new TrackComparator());

      // Source: playlists
      int[] playlistIds = playlist.getAutoFillRule().getSourcePlaylists();
      if (playlistIds != null) {
        for (int playlistId : playlistIds) {
          Playlist sourcePlaylist = this.playlistService.getPlaylistRegistry().getPlaylist(playlistId);
          if (sourcePlaylist != null) {
            for (Entry entry : sourcePlaylist.getEntries()) {
              if (tracks.containsKey(entry.getTrackId())) {
              	if(!playlist.getAutoFillRule().isDuplicatesFromPlaylists()) {
	              	// remove previous entry
	              	trackList.remove(entry.getTrack());
              	}
              }
              tracks.put(entry.getTrackId(), entry.getTrack());
              trackList.add(entry.getTrack());
            }
          }
        }
      }

      if (playlist.getEntries().size() > 0 && trackList.size() == 0) {
        throw new MissingSourceTracksException(playlist.getName());
      }

      ArrayList<BasicTrack> playlistTracks = new ArrayList<BasicTrack>();

      NewsTrackOption newsOption = playlist.getAutoFillRule().getNewsTrack();

      if (playlist.getAutoFillRule().isIncludeNews()) {
        // Add the appropriate news/weather track(s) at the beginning
        switch (newsOption) {
          case NEWS_WITH_WEATHER: {
            BasicTrack newsWeatherTrack = trackService.getTrack(TrackRegistry.LAUTFM_NEWS_WEATHER_ID);
            if (newsWeatherTrack != null) {
              playlistTracks.add(newsWeatherTrack);
            }
            break;
          }
          case NEWS: {
            BasicTrack newsTrack = trackService.getTrack(TrackRegistry.LAUTFM_NEWS_ID);
            if (newsTrack != null) {
            	playlistTracks.add(newsTrack);
            }
            break;
          }
          case WEATHER: {
            BasicTrack weatherTrack = trackService.getTrack(TrackRegistry.LAUTFM_WEATHER_ID);
            if (weatherTrack != null) {
            	playlistTracks.add(weatherTrack);
            }
            break;
          }
          case NEWS_AND_WEATHER: {
            // Add news track at beginning; weather track is handled after music tracks
            // unless weather was already at the beginning of the original playlist
            BasicTrack newsTrack = trackService.getTrack(TrackRegistry.LAUTFM_NEWS_ID);
            if (newsTrack != null) {
            	playlistTracks.add(newsTrack);
            }
            if (weatherWasAtBeginning) {
              // Preserve weather at beginning as well
              BasicTrack weatherTrack = trackService.getTrack(TrackRegistry.LAUTFM_WEATHER_ID);
              if (weatherTrack != null) {
                playlist.addTrack(weatherTrack);
              }
            }
            break;
          }
        }
      }
      

      // Add all music/remaining tracks
      for (BasicTrack track : trackList) {
        playlistTracks.add(track);
      }

      PlaylistProfile profile = playlistService.getProfile(playlist.getProfileId());

      // add ad separator / ad trigger
      if (playlist.getAutoFillRule().isIncludeAdTrigger() && profile != null && profile.getAdTrigger() != null && profile.getAdTrigger().getPos1() > -1) {
        if (profile.getAdTrigger().getSeperatorId() > 0) {
          BasicTrack adSeparator = trackRegistry.getTrack(profile.getAdTrigger().getSeperatorId());
          if (adSeparator != null) {
          	playlistTracks.add(adSeparator);
          }
        }
        BasicTrack adTrigger = profile.getAdTrigger().getTriggerId() > 0 ? trackRegistry.getTrack(profile.getAdTrigger().getTriggerId()) : trackRegistry.getStandardAdTrigger();
        if (adTrigger != null) {
        	playlistTracks.add(adTrigger);
        }
      }

      // add jingles from track rules
      if (playlist.getAutoFillRule().isIncludeTrackRules() && profile != null && profile.getTrackRules() != null) {
        for (TrackRule rule : profile.getTrackRules().getRules()) {
          BasicTrack track = trackRegistry.getTrack(rule.getTrackId());
          if (track != null && !tracks.containsKey(track.getId())) {
          	playlistTracks.add(track);
          }
        }
      }

      // For NEWS_AND_WEATHER: append weather track at the end if it was NOT at the beginning
      if (playlist.getAutoFillRule().isIncludeNews() && newsOption == NewsTrackOption.NEWS_AND_WEATHER && !weatherWasAtBeginning) {
        BasicTrack weatherTrack = trackService.getTrack(TrackRegistry.LAUTFM_WEATHER_ID);
        if (weatherTrack != null) {
        	playlistTracks.add(weatherTrack);
        }
      }
      
      // Now build the final playlist
      playlist.removeEntries(new ArrayList<Entry>(playlist.getEntries()));
      // start with start tracks and re-insert in original order if they still exist
      for (BasicTrack track : startTracks) {
				if(playlistTracks.remove(track)) {
					playlist.addTrack(track);
				}
			}
      
      // then add remaining tracks
      for(BasicTrack track : playlistTracks) {
      	playlist.addTrack(track);
      }
       
    }
  }

  /**
   * Fills all playlists for which autofill is enabled
   * 
   * @return list of filled playlists
   * @throws IOException
   */
  public List<Playlist> fillPlaylists() throws IOException {
    List<Playlist> playlists = new ArrayList<>();
    for (Playlist playlist : this.playlistService.getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE)) {
      if (playlist.getAutoFillRule().isEnabled()) {
        try {
          fillPlaylist(playlist);
          playlists.add(playlist);
        } catch (MissingSourceTracksException e) {
        }
      }
    }
    return playlists;
  }

}
