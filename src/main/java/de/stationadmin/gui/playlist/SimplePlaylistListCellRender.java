package de.stationadmin.gui.playlist;

import java.awt.Component;

import javax.swing.JList;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;

/**
 * Simple list cell renderer for playlists - includes name
 * 
 * @author korf
 */
public class SimplePlaylistListCellRender extends PlaylistListCellRenderer {
  private static final long serialVersionUID = 2149173739443393500L;

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
      boolean cellHasFocus) {
    Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if (value instanceof Playlist) {
      Playlist playlist = (Playlist) value;
      this.setText(playlist.getDisplayName());
      if (this.renderColors && playlist.getType() == PlaylistType.ONLINE) {
        this.setIcon(this.getColorIcon(playlist.getColor()));

      }
    }
    return comp;
  }

}
