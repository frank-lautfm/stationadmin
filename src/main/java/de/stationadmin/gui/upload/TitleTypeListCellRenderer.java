/**
 * 
 */
package de.stationadmin.gui.upload;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import de.stationadmin.gui.ClientContext;

/**
 *
 * @author Frank Korf
 *
 */
public class TitleTypeListCellRenderer extends DefaultListCellRenderer {
  private ClientContext ctx;
  
  public TitleTypeListCellRenderer(ClientContext ctx) {
    super();
    this.ctx = ctx;
  }

  
  /**
   * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList,
   *      java.lang.Object, int, boolean, boolean)
   */
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    this.setText(ctx.getString("title.type." + value));
    return comp;
  }

}
