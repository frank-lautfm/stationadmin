package de.stationadmin.gui.util;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import de.stationadmin.gui.TextProvider;

public class EnumTableCellRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = -7101725573503446982L;
  private TextProvider textProvider;
  private String keyPrefix;
  
  public EnumTableCellRenderer(TextProvider textProvider, String keyPrefix) {
    super();
    this.textProvider = textProvider;
    this.keyPrefix = keyPrefix;
  }
  
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    if(value != null) {
      String key = keyPrefix + "." + value.toString().toLowerCase();
      setText(textProvider.getString(key));
    }
    return this;
  }

}
