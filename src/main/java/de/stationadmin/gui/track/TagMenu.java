/**
 * 
 */
package de.stationadmin.gui.track;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;

import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.Tag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.gui.TextProvider;

/**
 * 
 * @author Frank Korf
 * 
 */
public class TagMenu extends JMenu {
  private static final long serialVersionUID = 7875690274224995784L;
  private TagManager tagManager;
  private boolean tag = true;
  private List<TrackTagAction> actions = new ArrayList<TrackTagAction>();
  private int[] titleIds = null;
  private TextProvider textProvider;

  public TagMenu(TextProvider textProvider, TagManager tagManager, boolean tag) {
    super(textProvider.getString("action." + (tag ? "tag" : "untag")));
    this.textProvider = textProvider;
    this.tag = tag;
    this.tagManager = tagManager;
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

    Map<String, JMenu> groupsMenus = new HashMap<String, JMenu>();

    for (String group : this.tagManager.getGroups(true)) {
      JMenu menu = new JMenu(group);
      this.add(menu);
      groupsMenus.put(group, menu);
    }
    
    JMenu dateFilterMenu = null;
    JMenu groupingMenu = null;

    for (String tagName : tagManager.getTags()) {
      Tag tag = this.tagManager.getTag(tagName);
      if (tag instanceof StaticTag) {
      	StaticTag stag = (StaticTag)tag;
        TrackTagAction action = new TrackTagAction(this.tagManager, textProvider, tagName, this.tag);
        actions.add(action);
        String group = tag.getGroup();
        JMenu menu = group != null ? groupsMenus.get(group) : null;
        if(menu == null) {
        	if(stag.isDateFilterTag()) {
        		if(dateFilterMenu == null) {
        			dateFilterMenu = new JMenu(this.textProvider.getString("titletagmanager.tagtype.date"));
        		}
        		menu = dateFilterMenu;
        	}
        	else if(stag.isGroupingTag()) {
        		if(groupingMenu == null) {
        			groupingMenu = new JMenu(this.textProvider.getString("titletagmanager.tagtype.grouping"));
        		}
        		menu = groupingMenu;
        	}
        }
        if (menu == null) {
          this.add(action);
        } else {
          menu.add(action);
        }
      }
    }
    if(groupingMenu != null || dateFilterMenu != null) {
    	if(this.getItemCount() > 0) this.insertSeparator(0);
      if(groupingMenu != null) this.insert(groupingMenu, 0);
      if(dateFilterMenu != null) this.insert(dateFilterMenu, 0);
    	
    }
    
    if (tag) {
      this.addSeparator();
      {
        TrackTagAction action = new TrackTagAction(this.tagManager, textProvider, null, this.tag);
        actions.add(action);
        this.add(action);
      }
    }

    // assign title ids
    for (TrackTagAction action : this.actions) {
      action.setTitleIds(titleIds);
    }

  }

  /**
   * @return the titleIds
   */
  public int[] getTitleIds() {
    return titleIds;
  }

  /**
   * @param titleIds
   *          the titleIds to set
   */
  public void setTitleIds(int[] titleIds) {
    this.titleIds = titleIds;
    for (TrackTagAction action : this.actions) {
      action.setTitleIds(titleIds);
    }
    this.setEnabled(titleIds != null && titleIds.length > 0);
  }

}
