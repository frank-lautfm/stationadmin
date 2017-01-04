/**
 * 
 */
package de.stationadmin.gui.upload;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.upload.QueuedTrack;
import de.stationadmin.gui.ClientContext;

/**
 * 
 * @author Frank Korf
 * 
 */
public class UploadedTrackTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 5054467725157073347L;
  private ClientContext ctx;
  private List<QueuedTrack> tracks = new ArrayList<QueuedTrack>();

  public UploadedTrackTableModel(ClientContext ctx) {
    super();
    this.ctx = ctx;
  }

  /**
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int columnIndex) {
    switch (Column.values()[columnIndex]) {
    case PRIVATE:
      return Boolean.class;
    case TYPE:
      return Integer.class;
    default:
      return String.class;
    }
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  /**
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    return ctx.getString("upload.title.column." + Column.values()[column].name().toLowerCase());
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return tracks.size();
  }

  /**
   * @return the titles
   */
  public List<QueuedTrack> getTracks() {
    return tracks;
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    DetailedTrack title = this.tracks.get(rowIndex).getTrack();
    Column col = Column.values()[columnIndex];
    switch (col) {
    case ALBUM:
      return title.getAlbum();
    case ARTIST:
      return title.getArtist();
    case GENRE:
      return title.getGenre();
    case PRIVATE:
      return title.isPrivateTrack();
    case TITLE:
      return title.getTitle();
    case TYPE:
      return title.getType();
    case YEAR:
      return title.getYear() > 0 ? Integer.toString(title.getYear()) : "";
    }
    return null;
  }

  /**
   * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
   */
  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    // Column col = Column.values()[columnIndex];
    // return col != Column.PRIVATE;
    return true;
  }

  /**
   * @param titles
   *          the titles to set
   */
  public void setTracks(List<QueuedTrack> tracks) {
    this.tracks = tracks;
    this.fireTableDataChanged();
  }

  /**
   * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
   */
  @Override
  public void setValueAt(Object value, int rowIndex, int columnIndex) {
    DetailedTrack title = this.tracks.get(rowIndex).getTrack();
    Column col = Column.values()[columnIndex];
    switch (col) {
    case ALBUM:
      title.setAlbum((String) value);
      break;
    case ARTIST:
      title.setArtist((String) value);
      break;
    case GENRE:
      title.setGenre((String) value);
      break;
    case PRIVATE:
      // if (((Boolean) value).booleanValue() == false) {
      title.setPrivateTrack((Boolean) value);
      // }
      break;
    case TITLE:
      title.setTitle((String) value);
      break;
    case TYPE:
      title.setType((Integer) value);
      break;
    case YEAR:
      try {
        title.setYear(Integer.parseInt((String) value));
      } catch (Exception e) {
        title.setYear(0);
      }
      break;
    }
    this.tracks.get(rowIndex).setModified(true);
  }

  public List<DetailedTrack> getModifiedTracks() {
    List<DetailedTrack> modifiedTracks = new ArrayList<DetailedTrack>();
    for (QueuedTrack track : this.tracks) {
      if (track.isModified()) {
        modifiedTracks.add(track.getTrack());
      }
    }
    return modifiedTracks;
  }

  public enum Column {
    ARTIST, TITLE, ALBUM, GENRE, YEAR, TYPE, PRIVATE

  }

}
