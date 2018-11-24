package de.stationadmin.gui.playlist.config;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import de.stationadmin.gui.TextProvider;

public class TagWeightListCellRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = 5550041553935707307L;
  private TextProvider textProvider;

  public TagWeightListCellRenderer(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
  }

  @Override
  public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    if (value != null && !value.equals(Integer.valueOf(0))) {
      setText(textProvider.getString("playlistcfg.property.generatePushTag.option." + value));
    } else {
      setText(" ");
    }
    return comp;
  }

}
