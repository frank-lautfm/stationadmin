package de.stationadmin.gui.util;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

public class HintLabel extends JLabel {
  private static final long serialVersionUID = -7734317164596486524L;

  public HintLabel(String text) {
    super(text);
    setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    setBackground(new Color(0xFF, 0xFF, 0xCC));
    setOpaque(true);
  }


}
