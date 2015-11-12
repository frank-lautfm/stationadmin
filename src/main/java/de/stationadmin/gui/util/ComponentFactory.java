/*
 * Copyright (c) 2005 Frank Korf
 */
package de.stationadmin.gui.util;

import java.awt.Font;

import javax.swing.JLabel;

/**
 * @author <a href="mailto:dev@emjoy.de">Frank Korf</a>
 */
public class ComponentFactory {
  public static Font defaultLabelFont;
  public static Font defaultLabelFontSmall;
  public static Font boldLabelFont;
  public static Font boldLabelFontSmall;
  public static Font italicLabelFont;

  static {
    JLabel label = new JLabel();
    ComponentFactory.defaultLabelFont = label.getFont();
    ComponentFactory.boldLabelFont = new Font(defaultLabelFont.getFamily(), Font.BOLD, defaultLabelFont.getSize());
    ComponentFactory.boldLabelFontSmall = new Font(defaultLabelFont.getFamily(), Font.BOLD, defaultLabelFont.getSize() - 2);
    ComponentFactory.defaultLabelFontSmall = new Font(defaultLabelFont.getFamily(), 0, defaultLabelFont.getSize() - 2);
    ComponentFactory.italicLabelFont = new Font(defaultLabelFont.getFamily(), Font.ITALIC, defaultLabelFont.getSize());
  }

  /**
   * Creates a label with a bold font
   * @param text
   * @return label
   */
  public static JLabel boldLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(ComponentFactory.boldLabelFont);
    return label;
  }

  /**
   * Creates a label with a small bold font
   * @param text
   * @return label
   */
  public static JLabel boldLabelSmall(String text) {
    JLabel label = new JLabel(text);
    label.setFont(ComponentFactory.boldLabelFontSmall);
    return label;
  }

  /**
   * Creates a label with a small font
   * @param text
   * @return label
   */
  public static JLabel defaultLabelSmall(String text) {
    JLabel label = new JLabel(text);
    label.setFont(ComponentFactory.defaultLabelFontSmall);
    return label;
  }
}
