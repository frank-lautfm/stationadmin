/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 * @author korf
 * 
 */
public class PopupListener extends MouseAdapter {
  private JComponent source;
  private JPopupMenu popup;

  public PopupListener(JComponent source, JPopupMenu popup) {
    super();
    this.source = source;
    this.popup = popup;
  }


  private void checkPopup(MouseEvent e) {
    if (e.isPopupTrigger()) {
      popup.show(source, e.getX(), e.getY());
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    this.checkPopup(e);
  }

  /**
   * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
   */
  @Override
  public void mousePressed(MouseEvent e) {
    this.checkPopup(e);
  }

  /**
   * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseReleased(MouseEvent e) {
    this.checkPopup(e);
  }

}
