/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.gui.TextProvider;

/**
 * @author Frank
 * 
 */
public class TagHighlightMenu extends JMenu {
  private static final long serialVersionUID = 8673965995995644976L;
  private TagManager tagManager;
  private ValueModel taggedTitlesHolder;
  private Set<String> selected = new HashSet<String>();
  private ItemChangeListener listener = new ItemChangeListener();
  private TextProvider textProvider;

  public TagHighlightMenu(TextProvider textProvider, TagManager tagManager, ValueModel taggedTitlesHolder) {
    super(textProvider.getString("action.taghighlight"));
    this.tagManager = tagManager;
    this.textProvider = textProvider;
    this.taggedTitlesHolder = taggedTitlesHolder;
    this.rebuild();

    tagManager.addPropertyChangeListener("tags", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        rebuild();
      }

    });
  }

  private void rebuild() {
    this.removeAll();
    for (String tagName : tagManager.getTags()) {
      JCheckBoxMenuItem item = new JCheckBoxMenuItem(tagName);
      item.putClientProperty("tag", tagName);
      item.addChangeListener(listener);
      this.add(item);
    }

    this.addSeparator();
    {
      JCheckBoxMenuItem item = new JCheckBoxMenuItem(this.textProvider.getString("action.taghighlight.any"));
      item.putClientProperty("tag", "#any#");
      item.addChangeListener(listener);
      this.add(item);
    }
    {
      JCheckBoxMenuItem item = new JCheckBoxMenuItem(this.textProvider.getString("action.taghighlight.anystatic"));
      item.putClientProperty("tag", "#anystatic#");
      item.addChangeListener(listener);
      this.add(item);
    }

  }

  private void rebuildIdList() {
    if (selected.size() == 0) {
      this.taggedTitlesHolder.setValue(null);
    } else {
      BitSet bs = new BitSet();
      for (String tag : selected) {
        try {
          if (tag.equals("#any#")) {
            for (String t : this.tagManager.getTags()) {
              for (int id : this.tagManager.getTrackIds(t)) {
                bs.set(id);
              }
            }
          } else if (tag.equals("#anystatic#")) {
            for (StaticTag t : this.tagManager.getStaticTags()) {
              for (int id : this.tagManager.getTrackIds(t.getName())) {
                bs.set(id);
              }
            }

          } else {
            for (int id : this.tagManager.getTrackIds(tag)) {
              bs.set(id);
            }
          }
        } catch (IOException e) {
          e.printStackTrace(); // FIXME
        }
      }
      this.taggedTitlesHolder.setValue(bs);
    }

  }

  private class ItemChangeListener implements ChangeListener {

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
     * )
     */
    @Override
    public void stateChanged(ChangeEvent e) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
      String tag = (String) item.getClientProperty("tag");
      if (item.isSelected() && !selected.contains(tag)) {
        selected.add(tag);
        rebuildIdList();
      }
      if (!item.isSelected() && selected.contains(tag)) {
        selected.remove(tag);
        rebuildIdList();
      }

    }

  }

}
