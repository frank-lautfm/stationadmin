/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.apache.commons.io.IOUtils;

/**
 * @author Frank
 *
 */
public class AppUtils {

  private static JFrame rootFrame;
  private static Desktop desktop;
  
  static {
    if (Desktop.isDesktopSupported()) {
      desktop = Desktop.getDesktop();
    }
  }

  public static void centerWithinRoot(Component comp) {
    if (rootFrame != null) {
      SwingTools.centerWithin(rootFrame, comp);
    } else {
      SwingTools.centerOnScreen(comp);
    }
  }

  /**
   * @return the rootFrame
   */
  public static JFrame getRootFrame() {
    return rootFrame;
  }

  /**
   * @param rootFrame the rootFrame to set
   */
  public static void setRootFrame(JFrame rootFrame) {
    AppUtils.rootFrame = rootFrame;
  }

  /**
   * Gets an icon
   * 
   * @param name
   * @return
   */
  public static ImageIcon getIcon(String name) {
    InputStream stream = AppUtils.class.getClassLoader().getResourceAsStream("icons/" + name);
    if (stream != null) {
      try {
        return new ImageIcon(IOUtils.toByteArray(stream));
      } catch (IOException e) {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * @return the desktop
   */
  public static Desktop getDesktop() {
    return desktop;
  }

}
