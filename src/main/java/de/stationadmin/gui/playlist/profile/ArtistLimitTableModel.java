/**
 * 
 */
package de.stationadmin.gui.playlist.profile;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.value.ValueHolder;

import de.stationadmin.base.Settings;
import de.stationadmin.gui.TextProvider;


/**
 * Table model for {@link Settings#getGenerateArtistPreselectLimits()}
 * 
 * @author korf
 */
public class ArtistLimitTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 3121314489510345016L;
  private TextProvider textProvider;
  private Map<String, Integer> artistLimits = new HashMap<String, Integer>();
  private ValueHolder defaultLimit = new ValueHolder(0);

  private List<Entry> entries = new ArrayList<ArtistLimitTableModel.Entry>();

  /**
   * @param textProvider
   * @param artistLimits
   */
  public ArtistLimitTableModel(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;

    rebuildEntries();
    this.defaultLimit.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (defaultLimit.intValue() > 0) {
          ArtistLimitTableModel.this.artistLimits.put("*", defaultLimit.intValue());
        } else {
          ArtistLimitTableModel.this.artistLimits.remove("*");
        }

      }
    });

  }
  
  private void rebuildEntries() {
    this.entries.clear();
    for (java.util.Map.Entry<String, Integer> entry : artistLimits.entrySet()) {
      if (entry.getKey().equals("*")) {
        this.defaultLimit.setValue(entry.getValue());
      } else {
        this.entries.add(new Entry(entry.getKey(), entry.getValue()));
      }
    }
    Collections.sort(entries);
    entries.add(new Entry());
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    switch (columnIndex) {
    case 1:
      return Integer.class;
    default:
      return String.class;
    }
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public String getColumnName(int column) {
    switch (column) {
    case 0:
      return this.textProvider.getString("settings.playlistgen.table.artist");
    case 1:
      return this.textProvider.getString("settings.playlistgen.table.limit");
    default:
      return null;

    }
  }

  @Override
  public int getRowCount() {
    return entries.size();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {

    Entry entry = this.entries.get(rowIndex);
    switch (columnIndex) {
    case 0:
      return entry.artist;
    case 1:
      return entry.limit;
    }

    return null;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    Entry entry = this.entries.get(rowIndex);
    switch (columnIndex) {
    case 0:
      entry.artist = StringUtils.trimToNull(aValue.toString());
    case 1:
      try {
        entry.limit = aValue instanceof Integer ? ((Integer) aValue) : Integer.parseInt(aValue.toString());
      } catch (Exception e) {
      }
    }
    this.updateModel();
    if (this.entries.size() > 0 && this.entries.get(this.entries.size() - 1).artist != null) {
      this.entries.add(new Entry());
      this.fireTableRowsInserted(this.entries.size() - 1, this.entries.size() - 1);
    }
  }

  private void updateModel() {
    this.artistLimits.clear();
    if (this.defaultLimit.intValue() > 0) {
      this.artistLimits.put("*", this.defaultLimit.intValue());
    }
    for (Entry entry : this.entries) {
      if (StringUtils.isNotEmpty(entry.artist) && entry.limit > 0) {
        this.artistLimits.put(entry.artist, entry.limit);
      } 
    }
  }

  private static class Entry implements Comparable<Entry> {

    String artist;
    int limit;

    Entry() {

    }

    /**
     * @param artist
     * @param limit
     */
    Entry(String artist, int limit) {
      super();
      this.artist = artist;
      this.limit = limit;
    }

    @Override
    public int compareTo(Entry o) {
      return StringUtils.trimToEmpty(this.artist).compareToIgnoreCase(StringUtils.trimToEmpty(o.artist));
    }

  }

  /**
   * @return the artistLimits
   */
  public Map<String, Integer> getArtistLimits() {
    return artistLimits;
  }

  /**
   * @return the defaultLimit
   */
  public ValueHolder getDefaultLimit() {
    return defaultLimit;
  }

  public void setArtistLimits(Map<String, Integer> artistLimits) {
    this.artistLimits = artistLimits;
    this.rebuildEntries();
    this.fireTableDataChanged();
  }

}
