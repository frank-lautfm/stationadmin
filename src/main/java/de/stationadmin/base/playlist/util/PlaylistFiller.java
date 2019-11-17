package de.stationadmin.base.playlist.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stationadmin.base.Settings;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistRegistry;
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
  private Settings settings;
  private PlaylistRegistry playlistRegistry;
  private TrackService trackService;
  private TrackRegistry trackRegistry;
  private TagManager tagManager;

  public PlaylistFiller(Settings settings, PlaylistRegistry playlistRegistry, TrackService trackService, TagManager tagManager) {
    super();
    this.settings = settings;
    this.playlistRegistry = playlistRegistry;
    this.trackService = trackService;
    this.trackRegistry = trackService.getTrackRegistry();
    this.tagManager = tagManager;
  }

  /**
   * Fills a single playlist with tracks based on autofill rules (if enabled)
   * 
   * @param playlist playlist to fill
   * @throws IOException
   */
  public void fillPlaylist(Playlist playlist) throws IOException {
    if (playlist.getAutoFillRule().isEnabled()) {
      BasicTrack preNewsJingle = null;
      BasicTrack firstJingle = null;
      if (playlist.getEntries().size() > 0) {

        if (playlist.getEntries().size() > 1 && playlist.getEntry(0).getTrack().getType() == BasicTrack.TYPE_NEWS
            && playlist.getEntry(1).getTrack().getType() == BasicTrack.TYPE_JINGLE) {
          firstJingle = playlist.getEntry(1).getTrack();
        } else if (playlist.getEntries().size() > 2 && playlist.getEntry(0).getTrack().getType() == BasicTrack.TYPE_JINGLE
            && playlist.getEntry(1).getTrack().getType() == BasicTrack.TYPE_NEWS) {
          preNewsJingle = playlist.getEntry(0).getTrack();
          if (playlist.getEntry(2).getTrack().getType() == BasicTrack.TYPE_JINGLE) {
            firstJingle = playlist.getEntry(2).getTrack();
          }
        } else if (playlist.getEntry(0).getTrack().getType() == BasicTrack.TYPE_JINGLE) {
          firstJingle = playlist.getEntry(0).getTrack();
        }
      }

      playlist.removeEntries(new ArrayList<Entry>(playlist.getEntries()));
      Map<Integer, BasicTrack> tracks = new HashMap<>();

      // Source: Tgas
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

      // Source: playlists
      int[] playlistIds = playlist.getAutoFillRule().getSourcePlaylists();
      if (playlistIds != null) {
        for (int playlistId : playlistIds) {
          Playlist sourcePlaylist = this.playlistRegistry.getPlaylist(playlistId);
          if (sourcePlaylist != null) {
            for (Entry entry : sourcePlaylist.getEntries()) {
              if (!tracks.containsKey(entry.getTrackId())) {
                tracks.put(entry.getTrackId(), entry.getTrack());
              }
            }
          }
        }
      }

      // add tracks from source tags and playlists
      ArrayList<BasicTrack> trackList = new ArrayList<>(tracks.values());
      trackList.sort(new TrackComparator());

      if (playlist.getAutoFillRule().isIncludeNews()) {
        if (preNewsJingle != null) {
          if (trackList.remove(preNewsJingle)) {
            playlist.addTrack(preNewsJingle);
          }
        }
        BasicTrack newsTrack = trackService.getTrack(TrackRegistry.LAUTFM_NEWS_ID);
        if (newsTrack != null) {
          playlist.addTrack(newsTrack);
        }
      }

      if (firstJingle != null) {
        if (trackList.remove(firstJingle)) {
          playlist.addTrack(firstJingle);
        }
      }

      for (BasicTrack track : trackList) {
        playlist.addTrack(track);
      }

      // add ad separator / ad trigger
      if (playlist.getAutoFillRule().isIncludeAdTrigger() && settings.getAdTriggerPosition1() > -1) {
        if (settings.getAdSeparatorId() > 0) {
          BasicTrack adSeparator = trackRegistry.getTrack(settings.getAdSeparatorId());
          if (adSeparator != null) {
            playlist.addTrack(adSeparator);
          }
        }
        BasicTrack adTrigger = settings.getAdTriggerId() > 0 ? trackRegistry.getTrack(settings.getAdTriggerId()) : trackRegistry.getStandardAdTrigger();
        if (adTrigger != null) {
          playlist.addTrack(adTrigger);
        }
      }

      // add jingles from track rules
      if (playlist.getAutoFillRule().isIncludeTrackRules()) {
        for (TrackRule rule : settings.getTrackRules()) {
          BasicTrack track = trackRegistry.getTrack(rule.getTrackId());
          if (track != null) {
            playlist.addTrack(track);
          }
        }
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
    for (Playlist playlist : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
      if (playlist.getAutoFillRule().isEnabled()) {
        fillPlaylist(playlist);
        playlists.add(playlist);
      }
    }
    return playlists;
  }

}
