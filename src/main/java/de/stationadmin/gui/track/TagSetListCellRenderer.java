/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.Component;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import de.stationadmin.base.tag.TagSet;
import de.stationadmin.gui.TextProvider;

/**
 * @author Frank
 *
 */
public class TagSetListCellRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = -86251475999681027L;
  
  private TextProvider textProvider;

  /**
   * @param textProvider
   */
  public TagSetListCellRenderer(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
  }


  /**
   * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList,
   *      java.lang.Object, int, boolean, boolean)
   */
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
      boolean cellHasFocus) {
    Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if (value == null) {
      setText(textProvider.getString("tagset.default"));
    } else if (value instanceof TagSet) {
      setText(((TagSet)value).getName());
    }
    return comp;
  }

}
