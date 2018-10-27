/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.UIManager;

import org.apache.commons.io.IOUtils;

/**
 * @author Frank
 *
 */
public class AppUtils {

  private static JFrame rootFrame;
  private static Desktop desktop;
  private static boolean darkTheme = false;
  private static Color textBackgroundColor = Color.WHITE;

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

  public static boolean isDarkTheme() {
    return darkTheme;
  }

  public static void setLookAndFeel(String className) {
    if (className == null || className.equals("system")) {
      className = UIManager.getSystemLookAndFeelClassName();
    }
    try {
      UIManager.setLookAndFeel(className);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    Color background = UIManager.getColor("TextArea.background");
    darkTheme = background.getRed() + background.getGreen() + background.getBlue() < 255;
    if(darkTheme) {
      textBackgroundColor = background;
      TitledPanel.setUp(new Color(51, 51, 51), new Color(200, 200, 200));
    }
    else {
      textBackgroundColor = Color.WHITE;
    }
    
    // special colors for some themes
    if(className.contains("AcrylLookAndFeel")) {
      TitledPanel.setUp(new Color(75, 75, 80), new Color(200, 200, 200));
    }
    else if(className.contains("Nimbus")) {
      TitledPanel.setUp(new Color(51, 90, 130), new Color(200, 200, 200));
    }
    else if(className.contains("Aluminium")) {
      TitledPanel.setUp(new Color(51, 90, 130), new Color(200, 200, 200));
    }
    


  }

  public static Color getTextBackgroundColor() {
    return textBackgroundColor;
  }

}
