/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * 
 * @author Frank Korf
 */
public class MenuLabel extends JLabel {
  private static final long serialVersionUID = -8738864648534981994L;
  private ActionListener actionListener;

  /**
   * 
   */
  public MenuLabel() {
    this.init();
  }

  /**
   * @param text
   */
  public MenuLabel(String text) {
    this.setText(text);
    this.init();
  }

  private void init() {
    MouseHandler m = new MouseHandler();
    this.addMouseListener(m);
    this.addMouseMotionListener(m);
    setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
  }

  private class MouseHandler extends MouseAdapter {

    /**
     * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseClicked(MouseEvent e) {
      if (actionListener != null && isEnabled()) {
        actionListener.actionPerformed(new ActionEvent(this, (int) (System.currentTimeMillis() % 100000), "clicked"));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
      }
    }

    /**
     * @see java.awt.event.MouseAdapter#mouseEntered(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseEntered(MouseEvent e) {
      if (isEnabled()) {
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
      }
    }

    /**
     * @see java.awt.event.MouseAdapter#mouseExited(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseExited(MouseEvent e) {
      if (isEnabled()) {
        setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
      }
    }

  }

  /**
   * @return the actionListener
   */
  public ActionListener getActionListener() {
    return actionListener;
  }

  /**
   * @param actionListener
   *          the actionListener to set
   */
  public void setActionListener(ActionListener actionListener) {
    this.actionListener = actionListener;
  }

}
