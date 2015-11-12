/**
 * 
 */
package de.stationadmin.gui.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.playlist.shuffle.TagWeight;


/**
 * @author korf
 * 
 */
public class TagWeightTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -6300663078257883159L;
  private Map<String, Integer> entriesMap;
  private List<TagWeight> entries = new ArrayList<TagWeight>();
  private List<TagWeight> tagWeights = new ArrayList<TagWeight>();
  private boolean showMax = true;

  public TagWeightTableModel(Map<String, Integer> tagWeights) {
    for(Entry<String, Integer> entry : tagWeights.entrySet()) {
      this.tagWeights.add(new TagWeight(entry.getKey(), entry.getValue(), 1f));
    }
    this.entriesMap = tagWeights;
    this.showMax = false;
    this.rebuild();
  }

  public TagWeightTableModel(List<TagWeight> tagWeights) {
    this.tagWeights = tagWeights;
    this.rebuild();
  }

  @Override
  public int getColumnCount() {
    return this.showMax ? 3 : 2;
  }

  @Override
  public String getColumnName(int column) {
    // FIXME
    switch (column) {
    case 0:
      return "Tag";
    case 1:
      return "Gewichtung";
    case 2:
      return "Max";
    }
    return null;
  }

  @Override
  public int getRowCount() {
    return entries.size();
  }

  @Override
  public Object getValueAt(int row, int col) {
    TagWeight entry = this.entries.get(row);
    switch (col) {
    case 0:
      return entry.getTag();
    case 1:
      return entry.getWeight();
    case 2:
      return entry.getMaxFraction();
    }
    return null;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  public void rebuild() {
    this.entries.clear();
    Collections.sort(this.tagWeights);
    for(TagWeight w : this.tagWeights) {
      entries.add(new TagWeight(w.getTag(), w.getWeight(), w.getMaxFraction()));
    }
    entries.add(new TagWeight(null, 0, 1f));
    this.fireTableDataChanged();
  }

  void updateModel() {
    Set<String> used = new HashSet<String>();
    this.tagWeights.clear();
    for(TagWeight entry : this.entries) {
      if(StringUtils.isNotEmpty(entry.getTag()) && !used.contains(entry.getTag()) && entry.getWeight() != 0) {
        this.tagWeights.add(entry);
        used.add(entry.getTag());
        if(this.entriesMap != null) {
          this.entriesMap.put(entry.getTag(), entry.getWeight());
        }
      }
    }
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    TagWeight entry = this.entries.get(rowIndex);
    switch (columnIndex) {
    case 0:
      entry.setTag((String) aValue);
      break;
    case 1:
      if (aValue instanceof String) {
        try {
          aValue = new Integer((String) aValue);
        } catch (NumberFormatException e) {
        }
      }
      entry.setWeight((Integer) aValue);
      break;
    case 2:
      entry.setMaxFraction((Float) aValue);
      break;
    }
    updateModel();
    if (this.entries.get(this.entries.size() - 1).getTag() != null) {
      this.entries.add(new TagWeight(null, 0, 1f));
      this.fireTableRowsInserted(this.entries.size() - 1, this.entries.size() - 1);
    }
  }

  @Override
  public Class<?> getColumnClass(int col) {
    switch (col) {
    case 0:
      return String.class;
    case 1:
      return Integer.class;
    case 2:
      return Float.class;
    default:
      return super.getColumnClass(col);
    }
  }

}
