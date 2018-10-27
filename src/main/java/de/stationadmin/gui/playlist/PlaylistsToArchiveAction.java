package de.stationadmin.gui.playlist;

import java.awt.event.ActionEvent;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;

/**
 * TEMPORARY
 * @author fkorf
 *
 */
public class PlaylistsToArchiveAction extends AbstractAction {
  private static final long serialVersionUID = 1405117014362903492L;

  private ClientContext ctx;

  public PlaylistsToArchiveAction(ClientContext ctx) {
    super();
    this.putValue(Action.NAME, "Alle Playlists ins Archiv kopieren");
    this.ctx = ctx;

  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    HashSet<String> names = new HashSet<>();
    for (Playlist pl : ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ARCHIVED)) {
      names.add(pl.getName().toUpperCase());
    }

    for (Playlist source : ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE)) {
      String name = source.getName();

      if (names.contains(name.toUpperCase())) {
        int suffix = 2;
        name = source.getName() + " (" + suffix + ")";
        while (names.contains(name.toUpperCase())) {
          suffix++;
          name = source.getName() + " " + suffix;
        }
      }

      Playlist copy = new Playlist(source.getTrackRegistry(), PlaylistType.ARCHIVED);
      copy.setName(name);
      copy.setDescription(source.getDescription());
      copy.setColor(source.getColor());
      copy.setComment(source.getComment());
      for (Entry entry : source.getEntries()) {
        copy.addTrack(entry.getTrack());
      }
      
      names.add(copy.getName().toUpperCase());

      try {
        ctx.getAdminClient().getPlaylistService().savePlaylist(copy);
        ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().register(copy);
      } catch (Exception e) {
        JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "action.playlist.archive.error"));
      }
    }

  }

}
