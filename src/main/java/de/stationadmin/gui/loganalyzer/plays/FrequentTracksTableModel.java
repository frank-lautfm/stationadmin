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

import de.stationadmin.base.loganalyzer.ItemFrequency;
import de.stationadmin.base.loganalyzer.PlayStatistics;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class FrequentTracksTableModel extends AbstractTableModel {

  private static final long serialVersionUID = 6709327008918771272L;
  
  enum Column {
    FREQUENCY, ARTIST, TITLE, TAGS
  }
  
  private TextProvider textProvider;
  private TagManager tagManager;
  private List<ItemFrequency<BasicTrack>> items;
  private List<String> tagNames = new ArrayList<String>();

  public FrequentTracksTableModel(ClientContext ctx, PlayStatistics stats) {
    super();
    this.textProvider = ctx.getTextProvider();
    this.tagManager = ctx.getAdminClient().getTagManager();
    this.items = stats.getFrequentTracks();
    
    refreshTagNames();
    tagManager.addPropertyChangeListener("tags", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        refreshTagNames();
      }
    });
    
    stats.addPropertyChangeListener("frequentTracks", new PropertyChangeListener() {

      @Override
      @SuppressWarnings("unchecked")
      public void propertyChange(PropertyChangeEvent evt) {
        items = (List<ItemFrequency<BasicTrack>>) evt.getNewValue();
        fireTableDataChanged();

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

  @Override
  public int getRowCount() {
    return this.items != null ? this.items.size() : 0;
  }

  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    ItemFrequency<BasicTrack> item = this.items.get(rowIndex);
    Column col = Column.values()[columnIndex];
    switch (col) {
    case FREQUENCY:
      return item.getFrequency();
    case ARTIST:
      return item.getItem().getArtist();
    case TITLE:
      return item.getItem().getTitle();
    case TAGS:
      return getTags(item.getItem());
    }
    return null;
  }

  private String getTags(BasicTrack track) {
    try {
      int id = track.getId();
      StringBuilder buf = new StringBuilder();
      for (String tag : tagNames) {
        if (tagManager.isTagged(tag, id)) {
          if (buf.length() > 0) {
            buf.append(", ");
          }
          buf.append(tag);
        }
      }
      return buf.length() > 0 ? buf.toString() : null;
    } catch (Exception e) {
    }
    return null;
  }

  @Override
  public String getColumnName(int column) {
    Column col = Column.values()[column];
    return this.textProvider.getString("frequency.column." + col.name().toLowerCase());
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
    case FREQUENCY:
      return Integer.class;
    case TAGS:
      return String.class;
    default:
      return String.class;
    }
  }

}
