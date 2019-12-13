/**
 * 
 */
package de.stationadmin.gui.playlist.config.shuffle;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class TagSequenceRuleRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = -6106570160909764851L;
  private TextProvider textProvider;

  public TagSequenceRuleRenderer(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
  }

  /**
   * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList,
   *      java.lang.Object, int, boolean, boolean)
   */
  @Override
  public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

    if (value instanceof TagSequenceRule) {
      TagSequenceRule rule = (TagSequenceRule) value;

      StringBuilder buf = new StringBuilder();
      buf.append(this.textProvider.getString("playlistcfg.advice.rule.if") + " ");
      for (int i = 0; i < rule.getPattern().length; i++) {
        if (i > 0) {
          buf.append(", ");
        }
        buf.append("'" + rule.getPattern()[i] + "'");
      }
      if (!rule.isNot()) {
        buf.append(" " + this.textProvider.getString("playlistcfg.advice.rule.then") + " ");
      } else {
        buf.append(" " + this.textProvider.getString("playlistcfg.advice.rule.not") + " ");
      }
      buf.append("'" + rule.getNext() + "'");

      this.setText(buf.toString());
    }

    return comp;
  }

}
