/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.playlist.util.PlaylistEntry;
import de.stationadmin.base.track.Title;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.ClientContext;

/**
 * Table model for playlist entries as used by {@link PlaylistEntryListViewer}
 * 
 * @author Frank Korf
 * 
 */
public class PlaylistEntryTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 1278397961096364788L;

  public enum Column {
    ARTIST, TITLE, PLAYLIST, STARTTIME
  }

  private List<PlaylistEntry> entries = new ArrayList<PlaylistEntry>();
  private ClientContext ctx;

  public PlaylistEntryTableModel(ClientContext ctx) {
    super();
    this.ctx = ctx;
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  /**
   * @return the entries
   */
  public List<PlaylistEntry> getEntries() {
    return entries;
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return this.entries.size();
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Column col = Column.values()[columnIndex];
    PlaylistEntry entry = this.entries.get(rowIndex);
    Title title;
    switch (col) {
    case ARTIST:
      title = ctx.getAdminClient().getTitleRegistry().getTrack(entry.getEntry().getTrackId());
      return title.getArtist();
    case TITLE:
      title = ctx.getAdminClient().getTitleRegistry().getTrack(entry.getEntry().getTrackId());
      return title.getTitle();
    case PLAYLIST:
      return entry.getPlaylist().getDisplayName();
    case STARTTIME:
      return TimeFormat.format(entry.getEntry().getStart(), true);
    }
    return null;
  }

  /**
   * @param entries
   *          the entries to set
   */
  public void setEntries(List<PlaylistEntry> entries) {
    this.entries = entries;
    this.fireTableDataChanged();
  }
  
  public void removeEntries(List<PlaylistEntry> entries) {
    this.entries.removeAll(entries);
    this.fireTableDataChanged();
  }

  /**
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    return ctx.getString("playlistentryviewer.column." + Column.values()[column].name().toLowerCase());
  }

  /**
   * @return the ctx
   */
  public ClientContext getCtx() {
    return ctx;
  }

}
