/**
 * 
 */
package de.stationadmin.base.playlist.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.BasicTrack;

/**
 * Distributes titles to a list of given playlists such that
 * <ul>
 * <li>each title occurs only once in the list of playlists
 * <li>not more than 3 titles per artist are added to a single playlist
 * <li>each playlist has approximately the same number of titles
 * </ul>
 * 
 * @author Frank Korf
 * 
 */
public class TrackDistributor {

  public boolean distributeTitles(List<Playlist> playlists, List<BasicTrack> titles, List<BasicTrack> failedTitles) {
    PlaylistCtx[] playlistCtxs = new PlaylistCtx[playlists.size()];
    for (int i = 0; i < playlists.size(); i++) {
      playlistCtxs[i] = new PlaylistCtx(playlists.get(i));
    }

    for (BasicTrack title : titles) {
      boolean alreadyUsed = false;
      PlaylistCtx best = null;
      for (int i = 0; i < playlistCtxs.length && !alreadyUsed; i++) {
        PlaylistCtx ctx = playlistCtxs[i];
        if (ctx.contains(title.getId())) {
          alreadyUsed = true;
        } else {
          int myCount = ctx.countTitlesOfArtist(title.getArtist());
          int bestCount = best != null ? best.countTitlesOfArtist(title.getArtist()) : Integer.MAX_VALUE;
          if (best == null || myCount < bestCount || (myCount == bestCount && ctx.getNumTitles() < best.getNumTitles())) {
            best = playlistCtxs[i];
          }
        }
      }
      if(!alreadyUsed) {
        if(best != null) {
          best.addTitle(title);
        }
        else {
          failedTitles.add(title);
        }
        
      }
    }
    if (failedTitles.size() > 0) {
      return false;
    }

    for (PlaylistCtx ctx : playlistCtxs) {
      ctx.commit();
    }

    return true;
  }

  private static class PlaylistCtx {
    private Playlist playlist;
    private Set<Integer> titleIds = new HashSet<Integer>();
    private Map<String, List<BasicTrack>> titlesByArtist = new HashMap<String, List<BasicTrack>>();
    private List<BasicTrack> titlesToAdd = new ArrayList<BasicTrack>();

    PlaylistCtx(Playlist playlist) {
      this.playlist = playlist;
      for (Entry entry : playlist.getEntries()) {
        BasicTrack title = playlist.getTrackRegistry().getTrack(entry.getTrackId());
        if (title != null) {
          this.registerTitle(title);
        }
      }
    }

    private void registerTitle(BasicTrack title) {
      titleIds.add(title.getId());
      List<BasicTrack> titles = this.titlesByArtist.get(title.getArtist());
      if (titles == null) {
        titles = new ArrayList<BasicTrack>();
        this.titlesByArtist.put(title.getArtist(), titles);
      }
      titles.add(title);
    }

    boolean contains(int titleId) {
      return this.titleIds.contains(titleId);
    }

    int countTitlesOfArtist(String artist) {
      List<BasicTrack> titles = this.titlesByArtist.get(artist);
      return titles != null ? titles.size() : 0;
    }

    int getNumTitles() {
      return this.titleIds.size();
    }

    void addTitle(BasicTrack title) {
      this.registerTitle(title);
      this.titlesToAdd.add(title);
    }

    void commit() {
      for (BasicTrack title : this.titlesToAdd) {
        this.playlist.addTrack(title);
      }
    }

  }

}
