/**
 * 
 */
package de.stationadmin.gui.playlist.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.gui.TextProvider;



/**
 * @author korf
 * 
 */
public class ArtistAliasTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 4715149251183789240L;

  private static class Entry implements Comparable<Entry> {
    String artist;
    String alias;

    Entry() {

    }

    /**
     * @param artist
     * @param alias
     */
    Entry(String artist, String alias) {
      super();
      this.artist = artist;
      this.alias = alias;
    }

    @Override
    public int compareTo(Entry o) {
      return StringUtils.trimToEmpty(this.artist).compareToIgnoreCase(StringUtils.trimToEmpty(o.artist));
    }

  }

  private TextProvider textProvider;
  private Map<String, String> aliasMap;

  private List<Entry> entries = new ArrayList<ArtistAliasTableModel.Entry>();

  /**
   * @param textProvider
   * @param aliasMap
   */
  public ArtistAliasTableModel(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
    this.aliasMap = new HashMap<String, String>();
    updateEntries();


  }
  
  private void updateEntries() {
    this.entries.clear();
    for (java.util.Map.Entry<String, String> e : aliasMap.entrySet()) {
      this.entries.add(new Entry(e.getKey(), e.getValue()));
    }
    Collections.sort(this.entries);
    this.entries.add(new Entry());
    
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
      return this.textProvider.getString("settings.playlistgen.table.alias");
    default:
      return null;

    }
  }

  @Override
  public int getRowCount() {
    return this.entries.size();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Entry entry = this.entries.get(rowIndex);
    switch (columnIndex) {
    case 0:
      return entry.artist;
    case 1:
      return entry.alias;
    }
    return null;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    Entry entry = this.entries.get(rowIndex);
    switch (columnIndex) {
    case 0:
      entry.artist = aValue.toString().trim();
      break;
    case 1:
      entry.alias = aValue.toString().trim();
      break;
    }
    this.updateModel();
    if (this.entries.size() > 0 && this.entries.get(this.entries.size() - 1).artist != null) {
      this.entries.add(new Entry());
      this.fireTableRowsInserted(this.entries.size() - 1, this.entries.size() - 1);
    }

  }
  
  private void updateModel() {
    this.aliasMap.clear();
    for(Entry entry : this.entries) {
      if(StringUtils.isNotBlank(entry.artist) && StringUtils.isNotEmpty(entry.alias)) {
        aliasMap.put(entry.artist, entry.alias);
      }
    }
  }

  /**
   * @return the aliasMap
   */
  public Map<String, String> getAliasMap() {
    return aliasMap;
  }

  public void setAliasMap(Map<String, String> aliasMap) {
    this.aliasMap = aliasMap != null ? aliasMap : new HashMap<>();
    updateEntries();
    this.fireTableDataChanged();
  }

}
