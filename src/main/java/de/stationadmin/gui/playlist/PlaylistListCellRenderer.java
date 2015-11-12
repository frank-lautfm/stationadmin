/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;


/**
 * @author Frank
 *
 */
public abstract class PlaylistListCellRenderer extends DefaultListCellRenderer {
  protected boolean renderColors = false;
  private Map<String, Icon> iconCache = new HashMap<String, Icon>();

  protected Icon getColorIcon(String color) {
    if (color == null) {
      color = "#FFFFFF";
    }
    Icon icon = this.iconCache.get(color);
    if (icon == null) {
      int r = Integer.parseInt(color.substring(1, 3), 16);
      int g = Integer.parseInt(color.substring(3, 5), 16);
      int b = Integer.parseInt(color.substring(5), 16);
      Color c = new Color(r, g, b);
      icon = new ColorIcon(c);
      iconCache.put(color, icon);
    }
    return icon;

  }

  /**
   * @return the renderColor
   */
  public boolean isRenderColors() {
    return renderColors;
  }

  /**
   * @param renderColor the renderColor to set
   */
  public void setRenderColors(boolean renderColor) {
    this.renderColors = renderColor;
  }

  protected static class ColorIcon implements Icon {
    private Color color;

    /**
     * @param color
     */
    private ColorIcon(Color color) {
      super();
      this.color = color;
    }

    /* (non-Javadoc)
     * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
     */
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(this.color);
      g.fill3DRect(x, y, getIconWidth(), getIconHeight(), true);
    }

    /* (non-Javadoc)
     * @see javax.swing.Icon#getIconWidth()
     */
    @Override
    public int getIconWidth() {
      return 8;
    }

    /* (non-Javadoc)
     * @see javax.swing.Icon#getIconHeight()
     */
    @Override
    public int getIconHeight() {
      return 10;
    }

  }

}
