/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import de.stationadmin.gui.JumpHandler;

/**
 * 
 * @author Frank Korf
 */
public abstract class JumpLabel extends JLabel {
  private static final long serialVersionUID = -8738864648534981994L;
  private JumpHandler jumpHandler;

  /**
   * 
   */
  public JumpLabel(JumpHandler jumpHandler) {
    this.jumpHandler = jumpHandler;
    this.init();
  }

  /**
   * @param text
   */
  public JumpLabel(JumpHandler jumpHandler, String text) {
    this.jumpHandler = jumpHandler;
    this.setText(text);
    this.init();
  }

  private void init() {
    MouseHandler m = new MouseHandler();
    this.addMouseListener(m);
    this.addMouseMotionListener(m);
    setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
  }

  protected abstract Object getJumpTarget();

  private class MouseHandler extends MouseAdapter {

    /**
     * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseClicked(MouseEvent e) {
      Object target = getJumpTarget();
      if (target != null) {
        jumpHandler.jumpTo(target);
      }
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
