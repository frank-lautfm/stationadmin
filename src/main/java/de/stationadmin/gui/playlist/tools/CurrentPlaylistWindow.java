/**
 * 
 */
package de.stationadmin.gui.playlist.tools;

import java.awt.BorderLayout;
import java.awt.HeadlessException;

import javax.swing.JFrame;

import com.jgoodies.binding.value.ValueHolder;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.gui.ClientContext;
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
public class CurrentPlaylistWindow extends JFrame {
  private static final long serialVersionUID = -8958610268442701703L;

  public CurrentPlaylistWindow(ClientContext ctx, Playlist playlist) throws HeadlessException {
    super();
    ValueHolder playlistHolder = new ValueHolder(null, true);
    PlaylistViewer viewer = new PlaylistViewer(ctx, playlistHolder, true);
    viewer.setValidationEnabled(false);
    playlistHolder.setValue(playlist);
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(viewer, BorderLayout.CENTER);
    this.setSize(500, 600);
    this.setTitle("Aktuelle Playlist: " + playlist.getName());
    SwingTools.centerWithin(ctx.getRootWindow(), this);
  }

}
