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
public class ActionLabel extends JLabel {
  private static final long serialVersionUID = -8738864648534981994L;
  private ActionListener actionListener;

  /**
   * 
   */
  public ActionLabel(ActionListener actionListener) {
    this.actionListener = actionListener;
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
      actionListener.actionPerformed(new ActionEvent(this, (int)System.currentTimeMillis(), "executed"));
    }

    /**
     * @see java.awt.event.MouseAdapter#mouseEntered(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseEntered(MouseEvent e) {
      setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
    }

    /**
     * @see java.awt.event.MouseAdapter#mouseExited(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseExited(MouseEvent e) {
      setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
    }

  }

}
