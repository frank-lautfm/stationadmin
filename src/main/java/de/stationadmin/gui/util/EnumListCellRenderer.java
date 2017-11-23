package de.stationadmin.gui.util;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import de.stationadmin.gui.TextProvider;

public class EnumListCellRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = -1749114269949590209L;
  private TextProvider textProvider;
  private String keyPrefix;
  
  public EnumListCellRenderer(TextProvider textProvider, String keyPrefix) {
    super();
    this.textProvider = textProvider;
    this.keyPrefix = keyPrefix;
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if(value != null) {
      String key = keyPrefix + "." + value.toString().toLowerCase();
      setText(textProvider.getString(key));
    }
    return this;
  }

}
