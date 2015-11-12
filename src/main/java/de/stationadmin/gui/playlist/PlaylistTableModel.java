package de.stationadmin.gui.playlist;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.validation.GVLValidator;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.Title;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.TextProvider;

/**
 * Table model for entries of a single playlist
 *
 * @author Frank Korf
 * @see PlaylistViewer
 */
public class PlaylistTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 1065700886504441729L;

  private TextProvider textProvider;
  private ValueModel playlistHolder;
  private ValueModel selectionHolder;

  private Playlist playlist;
  private List<Entry> gvlValidationErrors = new ArrayList<Entry>();
  private ValueModel hasValidationErrors = new ValueHolder(Boolean.FALSE);

  private boolean validationEnabled = true;

  private ChangeListener changeListener = new ChangeListener() {

    @Override
    @SuppressWarnings("unchecked")
    public void stateChanged(ChangeEvent e) {
      Object oldSelection = selectionHolder.getValue();
      gvlValidationErrors.clear();
      if (playlist != null && playlist.isGvlCheck() && validationEnabled) {
        if (!playlist.isShuffle()) {
          new GVLValidator().validate(playlist, gvlValidationErrors);
        }
      }
      hasValidationErrors.setValue(gvlValidationErrors.size() > 0 ? Boolean.TRUE : Boolean.FALSE);
      fireTableDataChanged();
      if (oldSelection != null) {
        if (oldSelection instanceof Entry) {
          if (playlist.getEntries().indexOf(oldSelection) > -1) {
            selectionHolder.setValue(oldSelection);
          }
        }
        if (oldSelection instanceof List) {
          ArrayList<Entry> newSelection = new ArrayList<Entry>();
          for (Entry old : (List<Entry>) oldSelection) {
            if (playlist.getEntries().indexOf(old) > -1) {
              newSelection.add(old);
            }
          }
          selectionHolder.setValue(newSelection);
        }
      }
    }

  };

  public PlaylistTableModel(TextProvider textProvider, ValueModel playlistHolder, ValueModel selectionHolder) {
    super();
    this.textProvider = textProvider;
    this.playlistHolder = playlistHolder;
    this.playlistHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        unregisterChangeListener();
        setPlaylist((Playlist) evt.getNewValue());
        registerChangeListener();
      }
    });
    this.selectionHolder = selectionHolder;
    registerChangeListener();

  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
      case ENTRYNO :
        return Integer.class;
      default :
        return String.class;
    }
  }

  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  @Override
  public String getColumnName(int column) {
    return this.textProvider.getString("playlistviewer.column." + Column.values()[column].name().toLowerCase());
  }

  public Entry getEntryAt(int row) {
    if (row > -1 && row < playlist.getEntries().size()) {
      return playlist.getEntries().get(row);
    } else {
      return null;
    }

  }

  public Playlist getPlaylist() {
    return playlist;
  }

  @Override
  public int getRowCount() {
    return this.playlist != null ? this.playlist.getEntries().size() : 0;
  }

  public Title getTitleAt(int row) {
    if (row > -1 && row < playlist.getEntries().size()) {
      Entry entry = playlist.getEntries().get(row);
      return entry.getTrack();
    } else {
      return null;
    }
  }

  @Override
  public Object getValueAt(int row, int column) {
    Entry entry = playlist.getEntries().get(row);
    Title title = entry.getTrack();
    Column col = Column.values()[column];

    switch (col) {
      case ENTRYNO :
        return row + 1;
      case STARTTIME :
        return TimeFormat.format(entry.getStart(), true);
      case ARTIST :
        return title != null ? title.getArtist() : "<unknown>";
      case TITLE :
        return title != null ? title.getTitle() : "<unknown>";
      case LENGTH :
        return title != null ? TimeFormat.format(title.getLength(), false) : null;
      case GENRE:
        return title instanceof DetailedTrack ? ((DetailedTrack)title).getGenre() : null;
      case YEAR:
        return title instanceof DetailedTrack ? ((DetailedTrack)title).getYear() : 0;
      case TYPE :
        return title != null ? title.getType() : -1;
      case ADDED :
        return new Date(entry.getTimestamp());
    }

    return null;
  }

  public boolean isGVLValidationError(int row) {
    return this.gvlValidationErrors.contains(this.playlist.getEntry(row));
  }

  private void registerChangeListener() {
    if (this.playlist != null) {
      this.playlist.addChangeListener(this.changeListener);
    }
  }

  public void setPlaylist(Playlist playlist) {
    this.playlist = playlist;
    this.gvlValidationErrors.clear();
    if (this.playlist != null && playlist.isGvlCheck() && validationEnabled) {
      new GVLValidator().validate(playlist, this.gvlValidationErrors);
    }
    this.hasValidationErrors.setValue(this.gvlValidationErrors.size() > 0 ? Boolean.TRUE : Boolean.FALSE);
    this.fireTableDataChanged();
  }

  private void unregisterChangeListener() {
    if (this.playlist != null) {
      this.playlist.removeChangeListener(this.changeListener);
    }
  }

  public enum Column {
    ENTRYNO, TYPE, STARTTIME, ARTIST, TITLE, GENRE, YEAR, LENGTH, ADDED
  }

  /**
   * @return the validate
   */
  public boolean isValidationEnabled() {
    return validationEnabled;
  }

  /**
   * @param validate the validate to set
   */
  public void setValidationEnabled(boolean validate) {
    this.validationEnabled = validate;
  }

  /**
   * @return the hasValidationErrors
   */
  public ValueModel getHasValidationErrors() {
    return hasValidationErrors;
  }

  /**
   * @param hasValidationErrors the hasValidationErrors to set
   */
  public void setHasValidationErrors(ValueModel hasValidationErrors) {
    this.hasValidationErrors = hasValidationErrors;
  }

}
