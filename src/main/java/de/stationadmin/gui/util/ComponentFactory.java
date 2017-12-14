/*
 * Copyright (c) 2005 Frank Korf
 */
package de.stationadmin.gui.util;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.gui.TextProvider;

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

  private TextProvider textProvider;

  public ComponentFactory(TextProvider textProvider) {
    this.textProvider = textProvider;
  }

  private static int avgTableFontWidth = -1;

  public static int getTableFontWidth(int numChars) {
    if (avgTableFontWidth == -1) {
      Font font = UIManager.getFont("Table.font");
      JPanel panel = new JPanel();
      FontMetrics fm = panel.getFontMetrics(font);
      if (fm != null) {
        int sum = 0;
        int cnt = 0;
        int[] widths = fm.getWidths();
        for (int i = 58; i <= 90; i++) {
          sum += widths[i];
          cnt++;
        }
        avgTableFontWidth = sum / cnt;
      } else {
        System.out.println("no font metrics available");
        avgTableFontWidth = 7;
      }
    }
    // System.out.println(numChars + " => " + (avgTableFontWidth * numChars));
    return avgTableFontWidth * numChars;
  }

  public static int getTableColumnWidthDate() {
    return getTableFontWidth(15);
  }

  public static int getTableColumnWidthDateTine() {
    return getTableFontWidth(18);
  }

  public static int getTableColumnWidthTime() {
    return getTableFontWidth(10);
  }

  /**
   * Creates a label with a bold font
   * 
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
   * 
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
   * 
   * @param text
   * @return label
   */
  public static JLabel defaultLabelSmall(String text) {
    JLabel label = new JLabel(text);
    label.setFont(ComponentFactory.defaultLabelFontSmall);
    return label;
  }

  public JTextField createTextField(ValueModel valueModel) {
    return createTextField(valueModel, true, true);
  }

  public JTextField createTextField(ValueModel valueModel, boolean commitOnFocusLost) {
    return createTextField(valueModel, commitOnFocusLost, true);
  }

  public JTextField createTextField(ValueModel valueModel, boolean commitOnFocusLost, boolean withMenu) {
    final JTextField tf = BasicComponentFactory.createTextField(valueModel, commitOnFocusLost);

    if (withMenu) {
      this.addStandardMenu(tf);
    }

    return tf;
  }

  public JTextArea createTextArea(ValueModel valueModel) {
    return createTextArea(valueModel, true);
  }

  public JTextArea createTextArea(ValueModel valueModel, boolean withMenu) {
    final JTextArea tf = BasicComponentFactory.createTextArea(valueModel);

    if (withMenu) {
      this.addStandardMenu(tf);
    }

    return tf;
  }

  private void addStandardMenu(final JTextComponent tf) {

    Action copyAction = new ClipboardAction(textProvider, tf, TransferHandler.getCopyAction());
    Action cutAction = new ClipboardAction(textProvider, tf, TransferHandler.getCutAction());
    Action pasteAction = new ClipboardAction(textProvider, tf, TransferHandler.getPasteAction());

    final JPopupMenu popup = new JPopupMenu();
    popup.add(cutAction);
    popup.add(copyAction);
    popup.add(pasteAction);

    tf.addMouseListener(new MouseAdapter() {

      private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
          popup.show(tf, e.getX(), e.getY());
        }
      }

      public void mouseClicked(MouseEvent e) {
        checkPopup(e);
      }

      public void mousePressed(MouseEvent e) {
        checkPopup(e);
      }

      public void mouseReleased(MouseEvent e) {
        checkPopup(e);
      }
    });
  }
}
