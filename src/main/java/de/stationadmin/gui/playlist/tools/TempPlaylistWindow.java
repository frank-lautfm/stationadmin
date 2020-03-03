/**
 * 
 */
package de.stationadmin.gui.playlist.tools;

import java.awt.BorderLayout;
import java.awt.HeadlessException;

import com.jgoodies.binding.value.ValueHolder;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.playlist.PlaylistViewer;
import de.stationadmin.gui.util.SwingTools;

/**
 * Window with a temporary playlist.
 * <p>
 * This playlist has no name and cannot be saved. The window can be used to tag
 * titles or distribute them to other playlists.
 *
 * @author Frank Korf
 */
public class TempPlaylistWindow extends StationAdminFrame {
  private static final long serialVersionUID = -8958610268442701703L;

  public TempPlaylistWindow(ClientContext ctx) throws HeadlessException {
    super(ctx, "TempPlaylist");
    ValueHolder playlistHolder = new ValueHolder(null, true);
    PlaylistViewer viewer = new PlaylistViewer(ctx, playlistHolder);
    viewer.setValidationEnabled(false);
    Playlist playlist = new Playlist(ctx.getAdminClient().getTrackService().getTrackRegistry(), PlaylistType.TEMPORARY);
    playlist.setLocalShuffleAllowed(true);
    playlistHolder.setValue(playlist);
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(viewer, BorderLayout.CENTER);
    this.setTitle(ctx.getTextProvider().getString("playlistviewer.temp.title"));
  }

}
