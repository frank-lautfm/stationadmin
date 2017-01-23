package de.stationadmin.gui.tag;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.DefaultTableModel;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.tag.TagSet;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;

public class TagSetTableModel extends DefaultTableModel {
  private static final long serialVersionUID = 2128466633534945895L;

  private TextProvider textProvider;
  private TagManager tagManager;
  private ValueModel included;
  private ValueModel excluded;

  Set<String> includedSet = new HashSet<String>();
  Set<String> excludedSet = new HashSet<String>();

  
  private List<Entry> entries = new ArrayList<Entry>();

  TagSetTableModel(ClientContext ctx, PresentationModel<TagSet> pm) {
    this.textProvider = ctx.getTextProvider();
    this.tagManager = ctx.getAdminClient().getTagManager();
    this.included = pm.getBufferedModel("includeTags");
    this.excluded = pm.getBufferedModel("excludeTags");

    PropertyChangeListener rebuildListener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        rebuildEntries();
      }
    };
    this.tagManager.addPropertyChangeListener("tags", rebuildListener);
    pm.getBeanChannel().addValueChangeListener(rebuildListener);
    
    rebuildEntries();

  }

  private static class Entry {
    private String tag;
    private Boolean status;
  }

  private void rebuildEntries() {
    this.entries.clear();
    List<String> tags = new ArrayList<String>();
    tags.add(TagManager.USED_TITLES);
    tags.add(TagManager.UNUSED_TITLES);
    tags.addAll(this.tagManager.getTags());


    this.includedSet.clear();
    this.excludedSet.clear();
    if (this.included.getValue() != null) {
      includedSet.addAll(Arrays.asList((String[]) this.included.getValue()));
    }
    if (this.excluded.getValue() != null) {
      excludedSet.addAll(Arrays.asList((String[]) this.excluded.getValue()));
    }

    for (String tag : tags) {
      Entry e = new Entry();
      e.tag = tag;
      if (includedSet.contains(tag)) {
        e.status = Boolean.TRUE;
      }
      if (excludedSet.contains(tag)) {
        e.status = Boolean.FALSE;
      }
      this.entries.add(e);
    }

    this.fireTableDataChanged();

  }

  @Override
  public int getRowCount() {
    return this.entries != null ? this.entries.size() : 0;
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return column == 1;
  }

  @Override
  public Object getValueAt(int row, int column) {
    if (column == 0) {
      String tag = this.entries.get(row).tag;
      if(tag.equals(TagManager.UNUSED_TITLES)) {
        return textProvider.getString("titlelist.unused");
      }
      if(tag.equals(TagManager.USED_TITLES)) {
        return textProvider.getString("titlelist.used");
      }
      return tag;
    } else {
      return this.entries.get(row).status;

    }
  }

  @Override
  public void setValueAt(Object aValue, int row, int column) {
    if (column == 1) {
      String tag = this.entries.get(row).tag;
      Boolean value = (Boolean) aValue;
      this.entries.get(row).status = value;
      if (aValue == null) {
        this.excludedSet.remove(tag);
        this.includedSet.remove(tag);
      } else if (aValue.equals(Boolean.TRUE)) {
        this.excludedSet.remove(tag);
        this.includedSet.add(tag);
      } else if (aValue.equals(Boolean.FALSE)) {
        this.excludedSet.add(tag);
        this.includedSet.remove(tag);
      }
      included.setValue(this.includedSet.toArray(new String[includedSet.size()]));
      excluded.setValue(this.excludedSet.toArray(new String[excludedSet.size()]));
    }
  }


  /* (non-Javadoc)
   * @see javax.swing.table.DefaultTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    switch(column) {
    case 0:
      return textProvider.getString("tagset.column.tag");
    case 1:
      return textProvider.getString("tagset.column.visibility");
    }
    return super.getColumnName(column);
  }
}
