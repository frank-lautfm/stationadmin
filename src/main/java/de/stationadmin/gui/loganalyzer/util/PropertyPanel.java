/**
 * 
 */
package de.stationadmin.gui.loganalyzer.util;

import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class PropertyPanel extends JPanel {
  private static final long serialVersionUID = -540512084674880411L;

  public PropertyPanel(String label, ValueModel model) {
    this(label, model, null);
  }

  public PropertyPanel(String label, ValueModel model, String suffix) {
    this.setLayout(new FormLayout("pref,3dlu,min(pref;35dlu),pref", "2dlu,pref,2dlu"));
    this.add(new JLabel(label), new CellConstraints(1, 2));
    this.add(BasicComponentFactory.createLabel(model, NumberFormat.getIntegerInstance()), new CellConstraints(3, 2));
    if(suffix != null) {
      this.add(new JLabel(suffix), new CellConstraints(4,2));
    }
  }

}