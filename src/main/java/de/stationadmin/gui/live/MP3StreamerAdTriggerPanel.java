package de.stationadmin.gui.live;

import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.TextProvider;

public class MP3StreamerAdTriggerPanel extends JPanel {
  private ValueHolder insertAdTriggers = new ValueHolder(Boolean.FALSE);
  private ValueHolder adPosition1 = new ValueHolder(20);
  private ValueHolder adPosition2 = new ValueHolder(50);

  public MP3StreamerAdTriggerPanel(TextProvider textProvider, ValueHolder insertAdTriggers, ValueHolder adPosition1, ValueHolder adPosition2) {
    super();
    this.insertAdTriggers = insertAdTriggers;
    this.adPosition1 = adPosition1;
    this.adPosition2 = adPosition2;
    init(textProvider);
  }
  
  private void init(TextProvider textProvider) {
    this.setLayout(new FormLayout("pref,pref,pref,pref,pref","pref"));
    CellConstraints cc = new CellConstraints();
    JCheckBox adCb = BasicComponentFactory.createCheckBox(insertAdTriggers, textProvider.getString("mp3streamer.dlg.property.insertAds"));
    final JTextField pos1Tf = BasicComponentFactory.createIntegerField(adPosition1, 0);
    pos1Tf.setEditable((Boolean)insertAdTriggers.getValue());
    pos1Tf.setColumns(3);
    final JTextField pos2Tf = BasicComponentFactory.createIntegerField(adPosition2, 0);
    pos2Tf.setColumns(3);
    pos2Tf.setEditable((Boolean)insertAdTriggers.getValue());
    
    this.add(adCb, cc.xy(1, 1));
    this.add(pos1Tf, cc.xy(2, 1));
    this.add(new JLabel(" " + textProvider.getString("mp3streamer.dlg.property.insertAds.and") + " "), cc.xy(3, 1));
    this.add(pos2Tf, cc.xy(4, 1));
    this.add(new JLabel(" " + textProvider.getString("mp3streamer.dlg.property.insertAds.minutes") + " "), cc.xy(5, 1));
    
    insertAdTriggers.addValueChangeListener(new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        pos1Tf.setEditable((Boolean)evt.getNewValue());
        pos2Tf.setEditable((Boolean)evt.getNewValue());
      }
    });
    
  }


}
