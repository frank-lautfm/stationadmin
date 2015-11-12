/**
 * 
 */
package de.stationadmin.base.playlist;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.util.AbstractBean;
import de.stationadmin.lfm.backend.PlaylistHead;

/**
 * 
 * @author Frank Korf
 * 
 */
public class PlaylistModificationDetector extends AbstractBean {
  private static final Logger log = Logger.getLogger(PlaylistModificationDetector.class);
  private SessionCtx ctx;
  private PlaylistRegistry playlistRegistry;
  private boolean modified = false;
  private int[] modifiedPlaylistIds;

  public PlaylistModificationDetector(SessionCtx ctx, PlaylistRegistry playlistRegistry) {
    super();
    this.ctx = ctx;
    this.playlistRegistry = playlistRegistry;
  }

  public void check() {
    try {
      Set<Integer> ids = new HashSet<Integer>();
      Set<Integer> modifiedIds = new HashSet<Integer>();
      for (Playlist playlist : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
        ids.add(playlist.getId());
      }
      for (PlaylistHead head : ctx.getServer().getPlaylists(ctx.getStationId())) {
        Playlist playlist = this.playlistRegistry.getPlaylist(head.getId());
        if (playlist == null || head.getUpdatedAt().getTime() > playlist.getUpdatedAt().getTime()) {
          modifiedIds.add(head.getId());
        }
        ids.remove(head.getId());
      }
      if(ids.size() > 0) {
        // playlists have been deleted
        modifiedIds.addAll(ids);
      }
      
      if(modifiedIds.size() > 0) {
        this.modifiedPlaylistIds = new int[modifiedIds.size()];
        int idx = 0;
        for(Integer id : modifiedIds) {
          this.modifiedPlaylistIds[idx++] = id;
        }
      }
      setModified(modifiedIds.size() > 0);
      
    } catch (IOException e) {
      log.error("unable to check for modified playlists");
    }

  }

  public TimerTask getCheckTask() {

    return new TimerTask() {
      @Override
      public void run() {
        check();
      }

    };

  }

  /**
   * @return the modified
   */
  public boolean isModified() {
    return modified;
  }

  /**
   * @param modified
   *          the modified to set
   */
  public void setModified(boolean modified) {
    boolean old = this.modified;
    this.modified = modified;
    this.firePropertyChange("modified", old, modified);
  }
  
  public void markClean() {
    setModified(false);
    this.modifiedPlaylistIds = null;
  }

  public int[] getModifiedPlaylistIds() {
    return modifiedPlaylistIds;
  }

}
