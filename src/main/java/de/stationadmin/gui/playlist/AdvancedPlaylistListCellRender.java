package de.stationadmin.gui.playlist;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JList;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.util.AppUtils;

/**
 * Advanced list cell renderer for playlists - includes name and length, bigger
 * cells and coloured background for every even cell
 * 
 * @author korf
 */
public class AdvancedPlaylistListCellRender extends PlaylistListCellRenderer {

  private static final long serialVersionUID = 2149173739443393500L;

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
      boolean cellHasFocus) {
    Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if (value instanceof Playlist) {
      Playlist playlist = (Playlist) value;
      String txt = playlist.getDisplayName() + " (" + TimeFormat.format(playlist.getLength(), true) + ")";
      if (playlist.isModified()) {
        txt += " *";
      }
      this.setText(txt);

      if (this.renderColors) {
        this.setIcon(this.getColorIcon(playlist.getColor()));

      }

    }
    this.setBorder(BorderFactory.createEmptyBorder(5, 3, 5, 3));
    if (!AppUtils.isDarkTheme() && index % 2 == 0 && !isSelected) {
      this.setBackground(new Color(250, 250, 255));
    }
    return comp;
  }

}
