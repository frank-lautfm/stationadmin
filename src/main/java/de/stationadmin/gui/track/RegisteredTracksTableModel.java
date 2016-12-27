/**
 * 
 */
package de.stationadmin.gui.track;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.tag.TagSet;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackComparator;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class RegisteredTracksTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 8386716830573914408L;
  public static final String USED_TITLES = "#USED#";
  public static final String UNUSED_TITLES = "#UNUSED#";
  public static final String TAGGED_TITLES = "#TAGGED#";
  private TextProvider textProvider;
  private TrackRegistry trackRegistry;
  private TagManager tagManager;
  private List<RegisteredTrack> tracks;
  private ValueModel tagSet;
  private ValueModel tag;
  private ValueModel uploadedBy;
  private ValueModel invertTag;
  private ValueModel numTracks = new ValueHolder(0);
  private ValueModel length = new ValueHolder(0);
  

  public RegisteredTracksTableModel(TextProvider textProvidder, TrackRegistry titleRegistry,
      TagManager titleTagService, ValueModel tagSet, ValueModel tag, ValueModel invertTag, ValueModel updloadedBy) {
    super();
    this.textProvider = textProvidder;
    this.trackRegistry = titleRegistry;
    this.tagManager = titleTagService;
    this.tagSet = tagSet;
    this.tag = tag;
    this.invertTag = invertTag;
    this.uploadedBy = updloadedBy;

    this.tracks = this.filterTitles(this.trackRegistry.getAllTracks());
    Collections.sort(tracks, new TrackComparator());

    // update model if number of titles changes
    PropertyChangeListener changeListener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        tracks = filterTitles(RegisteredTracksTableModel.this.trackRegistry.getAllTracks());
        Collections.sort(tracks, new TrackComparator());
        int length = 0;
        for (RegisteredTrack title : tracks) {
          length += title.getLength();
        }
        fireTableDataChanged();
        numTracks.setValue(tracks.size());
        RegisteredTracksTableModel.this.length.setValue(length);
      }

    };
    this.trackRegistry.addPropertyChangeListener("numTracks", changeListener);
    this.tagSet.addValueChangeListener(changeListener);
    this.tag.addValueChangeListener(changeListener);
    this.invertTag.addValueChangeListener(changeListener);
    this.uploadedBy.addValueChangeListener(changeListener);
  }

  @SuppressWarnings("unchecked")
  private List<RegisteredTrack> filterTitles(List<RegisteredTrack> titles) {
    Set<String> tags = new HashSet<String>();
    if (this.tag.getValue() instanceof List) {
      tags.addAll((List<String>) this.tag.getValue());
    } else {
      String tag = (String) this.tag.getValue();
      if (tag != null) {
        if (tag.equals(TAGGED_TITLES)) {
          tags.addAll(this.tagManager.getTags());
        } else {
          tags.add(tag);
        }
      }
    }

    UploadFilter uploadFilter = (UploadFilter) this.uploadedBy.getValue();
    List<RegisteredTrack> filtered = new ArrayList<RegisteredTrack>();
    try {
      TagSet set = (TagSet)tagSet.getValue();
      BitSet tagSetBits = null;
      if(set != null) {
        tagSetBits = tagManager.getTrackIds(set);
      }
      
      BitSet bits = null;
      for (String tag : tags) {
        bits = this.markTitles(tag, bits);
      }
      for (RegisteredTrack title : titles) {
        boolean accepted = (bits == null || bits.get(title.getId())) && (tagSetBits == null || tagSetBits.get(title.getId()));
        if (((Boolean) invertTag.getValue()).booleanValue()) {
          accepted = !accepted;
        }
        if (accepted && uploadFilter != null && uploadFilter != UploadFilter.ANYBODY) {
          switch (uploadFilter) {
            case FOREIGN :
              accepted = accepted && !title.isOwnTrack();
              break;
            case USER_ALL :
              accepted = accepted && title.isOwnTrack();
              break;
            case USER_PRIVATE :
              accepted = accepted && title.isOwnTrack() && title.isPrivateTrack();
              break;
            case USER_PUBLIC :
              accepted = accepted && title.isOwnTrack() && !title.isPrivateTrack();
              break;
          }

        }
        if (accepted) {
          filtered.add(title);
        }
      }

      return filtered;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return titles;
  }

  private BitSet markTitles(String tag, BitSet bits) throws IOException {
    if (bits == null) {
      bits = new BitSet();
    }

    int[] ids = null;
    if (tag.equals(USED_TITLES)) {
      for (RegisteredTrack title : this.trackRegistry.getAllTracks()) {
        if (title.getPlaylistIds().size() > 0) {
          bits.set(title.getId());
        }
      }
      return bits;
    } else if (tag.equals(UNUSED_TITLES)) {
      for (RegisteredTrack title : this.trackRegistry.getAllTracks()) {
        if (title.getPlaylistIds().size() == 0) {
          bits.set(title.getId());
        }
      }
      return bits;
    } else {
      ids = tagManager.getTrackIds(tag);
      if (ids != null) {
        for (int id : ids) {
          bits.set(id);
        }
        return bits;
      }
    }

    return null;
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  @Override
  public String getColumnName(int column) {
    Column col = Column.values()[column];
    return this.textProvider.getString("titletable.column." + col.name().toLowerCase());
  }

  /**
   * @return the length
   */
  protected ValueModel getLength() {
    return length;
  }

  public ValueModel getNumTracks() {
    return this.numTracks;
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return this.tracks.size();
  }

  public RegisteredTrack getTrackAt(int row) {
    if (row > -1 && row < this.tracks.size()) {
      return tracks.get(row);
    } else {
      return null;
    }
  }
  
  public List<RegisteredTrack> getTracks() {
    return new ArrayList<RegisteredTrack>(this.tracks);
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    RegisteredTrack track = this.tracks.get(rowIndex);
    Column col = Column.values()[columnIndex];

    switch (col) {
      case ARTIST :
        return track.getArtist();
      case ID:
        return track.getId();
      case TITLE :
        return track.getTitle();
      case ALBUM :
        return track.getAlbum();
      case GENRE :
        return track.getGenre();
      case YEAR :
        return track.getYear();
      case LENGTH :
        return TimeFormat.format(track.getLength(), false);
      case TYPE :
        return track.getType();
      case NUM_PLAYLISTS :
        return track.getPlaylistStatistics();
      case UPLOAD :
        return track.getUploadDate();
    }

    return null;
  }

  /**
   * @param length
   *          the length to set
   */
  protected void setLength(ValueModel length) {
    this.length = length;
  }

  /**
   * @param numTitles
   *          the numTitles to set
   */
  protected void setNumTracks(ValueModel numTitles) {
    this.numTracks = numTitles;
  }

  public enum UploadFilter {
    ANYBODY, FOREIGN, USER_ALL, USER_PUBLIC, USER_PRIVATE,
  }

  public enum Column {
    TYPE, ID, ARTIST, TITLE, ALBUM, LENGTH, GENRE, YEAR, UPLOAD, NUM_PLAYLISTS
  }

  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch(col) {
    case ARTIST:
    case TITLE:
    case ALBUM:
    case GENRE:
      return String.class;
    case TYPE:
    case LENGTH:
    case YEAR:
    case NUM_PLAYLISTS:
    case ID:
      return Integer.class;
    case UPLOAD:
      return Date.class;
    }
    return super.getColumnClass(columnIndex);
  }

}
