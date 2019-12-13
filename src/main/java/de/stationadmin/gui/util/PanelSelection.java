package de.stationadmin.gui.util;

import javax.swing.JPanel;

public class PanelSelection {
  private String label;
  private JPanel panel;

  /**
   * @param label
   * @param panel
   */
  public PanelSelection(String label, JPanel panel) {
    super();
    this.label = label;
    this.panel = panel;
  }

  public String toString() {
    return this.label;
  }

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @return the panel
   */
  public JPanel getPanel() {
    return panel;
  }

}
