/**
 * 
 */
package de.stationadmin.gui.loganalyzer.plays;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.loganalyzer.ItemFrequency;
import de.stationadmin.base.loganalyzer.Play;
import de.stationadmin.base.loganalyzer.PlayStatistics;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.util.TagRenameCommand;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;

import com.jgoodies.binding.value.ValueModel;

/**
 * @author korf
 *
 */
public class FrequentTagsTableModel extends AbstractTableModel {

  private static final long serialVersionUID = 6709327008918771273L;
  private TextProvider textProvider;
  private TagManager tagManager;
  private ValueModel playsHolder;
  private List<ItemFrequency<String>> items = new ArrayList<ItemFrequency<String>>();
  private List<String> tagNames = new ArrayList<String>();

  public FrequentTagsTableModel(ClientContext ctx, ValueModel playsHolder) {
    super();
    this.textProvider = ctx.getTextProvider();
    this.tagManager = ctx.getAdminClient().getTagManager();
    this.playsHolder = playsHolder;
    
    refreshTagNames();
    tagManager.addPropertyChangeListener("tags", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        refreshTagNames();
        updateTagFrequencies();
      }
    });
    
    playsHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updateTagFrequencies();
      }
    });
    
    updateTagFrequencies();
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
  
  @SuppressWarnings("unchecked")
  private void updateTagFrequencies() {
    HashMap<String, ItemFrequency<String>> tags = new HashMap<String, ItemFrequency<String>>();
    
    try {
      List<Play> plays = (List<Play>) playsHolder.getValue();
      if (plays != null) {
        for (Play play : plays) {
          try {
            int trackId = play.getTrack().getId();
            for (String tagName : tagNames) {
              if (tagManager.isTagged(tagName, trackId)) {
                ItemFrequency<String> tagFreq = tags.get(tagName);
                if (tagFreq == null) {
                  tags.put(tagName, new ItemFrequency<String>(tagName, 1));
                } else {
                	tagFreq.inc();
                }
              }
            }
          } catch (Exception e) {
            // Ignore individual track errors
          }
        }
      }
    } catch (Exception e) {
      // Ignore errors
    }
    
    items = new ArrayList<ItemFrequency<String>>(tags.values());
    Collections.sort(items);
    fireTableDataChanged();
  }

  @Override
  public int getRowCount() {
    return this.items != null ? this.items.size() : 0;
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    ItemFrequency<String> item = this.items.get(rowIndex);
    switch (columnIndex) {
    case 0:
      return item.getFrequency();
    case 1:
      return item.getItem();

    }
    return null;
  }

  @Override
  public String getColumnName(int column) {
    return this.textProvider.getString(column == 0 ? "frequency.column.frequency" : "frequency.column.tag");
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if(columnIndex == 0) {
      return Integer.class;
    }
    else {
      return String.class;
    }
  }

}
