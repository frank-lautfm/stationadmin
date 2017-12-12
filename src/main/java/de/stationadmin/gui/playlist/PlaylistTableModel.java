package de.stationadmin.gui.playlist;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
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
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.BasicTrack;
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

  private List<String> tagNames = new ArrayList<String>();
  private TagManager tagManager;

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

  public PlaylistTableModel(TextProvider textProvider, ValueModel playlistHolder, ValueModel selectionHolder, TagManager tagManager) {
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
    this.tagManager = tagManager;
    registerChangeListener();
    refreshTagNames();
    tagManager.addPropertyChangeListener("tags", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        refreshTagNames();
      }
    });

  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
    case ENTRYNO:
    case NUMPLAYLISTS:
    case YEAR:
    case TYPE:
      return Integer.class;
    case ADDED:
      return Date.class;
    default:
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

  public BasicTrack getTitleAt(int row) {
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
    BasicTrack track = entry.getTrack();
    Column col = Column.values()[column];

    switch (col) {
    case ENTRYNO:
      return row + 1;
    case STARTTIME:
      return TimeFormat.format(entry.getStart(), true);
    case ARTIST:
      return track != null ? track.getArtist() : "<unknown>";
    case TITLE:
      return track != null ? track.getTitle() : "<unknown>";
    case ALBUM:
      return track instanceof DetailedTrack ? ((DetailedTrack) track).getAlbum() : null;
    case LENGTH:
      return track != null ? TimeFormat.format(track.getLength(), false) : null;
    case GENRE:
      return track instanceof DetailedTrack ? ((DetailedTrack) track).getGenre() : null;
    case YEAR:
      return track instanceof DetailedTrack ? ((DetailedTrack) track).getYear() : 0;
    case TYPE:
      return track != null ? track.getType() : -1;
    case ADDED:
      return new Date(entry.getTimestamp());
    case NUMPLAYLISTS:
      if (track instanceof RegisteredTrack) {
        return ((RegisteredTrack) track).getPlaylistStatistics().getNumberOfPlaylistsTotal();
      } else {
        return 0;
      }
    case TAGS:
      if (track instanceof RegisteredTrack) {
        return getTags((RegisteredTrack)track);
      } else {
        return null;
      }
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
    ENTRYNO, TYPE, STARTTIME, ARTIST, TITLE, ALBUM, GENRE, YEAR, LENGTH, ADDED, NUMPLAYLISTS, TAGS
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

  private void refreshTagNames() {
    try {
      tagNames.clear();
      for (StaticTag tag : tagManager.getStaticTags()) {
        tagNames.add(tag.getName());
      }
      Collections.sort(tagNames);
    } catch (Exception e) {

    }
  }

  private String getTags(RegisteredTrack track) {
    try {
      int id = track.getId();
      int cnt = 0;
      if (track.getTagCnt() > 0) {
        StringBuilder buf = new StringBuilder();
        for (String tag : tagNames) {
          if (tagManager.isTagged(tag, id)) {
            if (buf.length() > 0) {
              buf.append(", ");
            }
            buf.append(tag);
            cnt++;
            if (cnt == track.getTagCnt()) {
              break;
            }
          }
        }
        return buf.toString();
      }
    } catch (Exception e) {
    }
    return null;
  }

}
