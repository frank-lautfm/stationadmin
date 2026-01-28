/**
 *
 */
package de.stationadmin.gui.loganalyzer.plays;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.TextProvider;

/**
 * Table model for unplayed titles
 * 
 * @author korf
 */
public class TrackTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 8659991094229248340L;

  enum Column {
    ARTIST, TITLE, LENGTH, TAGS
  }

  private TextProvider textProvider;
  private TagManager tagManager;
  private List<RegisteredTrack> tracks = new ArrayList<RegisteredTrack>();
  private List<String> tagNames = new ArrayList<String>();

  public TrackTableModel(List<RegisteredTrack> titles) {
    super();
    this.tracks = titles;
  }

  public TrackTableModel(TextProvider textProvider, TagManager tagManager) {
    super();
    this.textProvider = textProvider;
    this.tagManager = tagManager;
    
    refreshTagNames();
    tagManager.addPropertyChangeListener("tags", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        refreshTagNames();
      }
    });
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

  /*
   * (non-Javadoc)
   * 
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

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return tracks.size();
  }

  public List<RegisteredTrack> getTracks() {
    return tracks;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    RegisteredTrack title = this.tracks.get(rowIndex);
    Column col = Column.values()[columnIndex];
    switch (col) {
    case ARTIST:
      return title.getArtist();
    case TITLE:
      return title.getTitle();
    case LENGTH:
      return TimeFormat.format(title.getLength(), false);
    case TAGS:
      return getTags(title);
    }
    return null;
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

  public void setTracks(List<RegisteredTrack> titles) {
    this.tracks = titles;
    this.fireTableDataChanged();
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
    case TAGS:
      return String.class;
    default:
      return String.class;
    }
  }

}
