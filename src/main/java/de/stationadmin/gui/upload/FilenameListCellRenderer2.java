package de.stationadmin.gui.upload;

import java.awt.Component;
import java.io.File;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 * @author Frank
 *
 */
public class FilenameListCellRenderer2 extends DefaultListCellRenderer {
  private static final long serialVersionUID = 7114231066117583356L;

  /**
   * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
   */
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    File file = (File) value;
    setText(file.getName());
    return this;
  }
}