package de.stationadmin.gui.playlist.profile;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.playlist.shuffle.PlaylistProfileType;
import de.stationadmin.base.playlist.shuffle.WordDistributionStrategy;
import de.stationadmin.gui.TextProvider;

public class ProfileGeneralPanel extends JPanel {
  private static final long serialVersionUID = -7589155890230394048L;
  private TextProvider textProvider;
  private PlaylistProfileModel model;

  public ProfileGeneralPanel(TextProvider textProvider, PlaylistProfileModel model) {
    this.textProvider = textProvider;
    this.model = model;
    this.init();
  }

  @SuppressWarnings("unchecked")
  private void init() {

    this.setLayout(
        new FormLayout("5dlu,70dlu,5dlu,pref,2dlu,pref,pref,pref:grow,5dlu", "5dlu,pref,5dlu,pref,8dlu,pref,3dlu,pref,3dlu,pref,3dlu,pref,10dlu,pref,10dlu,pref,5dlu,pref,3dlu"));
    CellConstraints cc = new CellConstraints();

    int row = 2;

    JTextField nameTf = BasicComponentFactory.createTextField(model.getBufferedModel("name"));
    this.add(new JLabel(textProvider.getString("playlistprofilemanager.property.name")), cc.xy(2, row));
    this.add(nameTf, cc.xywh(4, row, 5, 1, CellConstraints.FILL, CellConstraints.CENTER));
    row += 2;

    final ValueHolder typeHolder = new ValueHolder();
    this.add(new JLabel(textProvider.getString("playlistprofilemanager.property.type")), cc.xy(2, row));
    JLabel typeTf = BasicComponentFactory.createLabel(typeHolder);
    this.add(typeTf, cc.xywh(4, row, 5, 1, CellConstraints.FILL, CellConstraints.CENTER));
    row += 2;

    model.getBufferedModel("type").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        PlaylistProfileType value = (PlaylistProfileType) evt.getNewValue();
        if (value != null) {
          typeHolder.setValue(textProvider.getString("playlistprofilemanager.property.type." + value.name().toLowerCase()));
        }
      }
    });

    final ValueHolder jingleIntervalEnable = new ValueHolder(false);
    jingleIntervalEnable.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (((Boolean) evt.getNewValue()).booleanValue() == false && !model.isInitializing()) {
          model.getBufferedModel("jingleInterval").setValue(0);
        }
      }

    });

    model.getBeanChannel().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        PlaylistProfile profile = (PlaylistProfile) evt.getNewValue();
        jingleIntervalEnable.setValue(profile != null && profile.getJingleInterval() > 0);
      }
    });

    final JCheckBox protect1stCb = BasicComponentFactory.createCheckBox(this.model.getBufferedModel("protectFirstJingle"), null);
    ValueModel protectAllJinglesModel = this.model.getBufferedModel("protectAllJingles");
    final JCheckBox protectAllCb = BasicComponentFactory.createCheckBox(protectAllJinglesModel, null);
    final JCheckBox jingleIntervalEnableCb = BasicComponentFactory.createCheckBox(jingleIntervalEnable, null);
    final JTextField jingleIntervalTf = BasicComponentFactory.createIntegerField(this.model.getBufferedModel("jingleInterval"), 0);
    jingleIntervalTf.setColumns(3);

    this.add(new JLabel(textProvider.getString("settings.property.shuffleJingle")), cc.xy(2, row));
    this.add(protect1stCb, cc.xy(4, row));
    this.add(new JLabel(textProvider.getString("settings.shuffle.protectFirstJingle")), cc.xywh(6, row, 3, 1));
    row += 2;

    this.add(jingleIntervalEnableCb, cc.xy(4, row));
    this.add(new JLabel(textProvider.getString("settings.shuffle.interval.every") + " "), cc.xy(6, row));
    this.add(jingleIntervalTf, cc.xy(7, row));
    this.add(new JLabel(" " + textProvider.getString("settings.shuffle.interval.minute")), cc.xy(8, row));
    row += 2;

    this.add(protectAllCb, cc.xy(4, row));
    this.add(new JLabel(textProvider.getString("settings.shuffle.protectAllJingles")), cc.xywh(6, row, 3, 1));
    row += 2;

    boolean enableJingleOptions = false;
    protect1stCb.setEnabled(enableJingleOptions);
    jingleIntervalEnableCb.setEnabled(enableJingleOptions);
    jingleIntervalTf.setEditable(enableJingleOptions);

    protectAllJinglesModel.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        boolean enable = evt.getNewValue() != null && !evt.getNewValue().equals(Boolean.TRUE);
        protect1stCb.setEnabled(enable);
        jingleIntervalEnableCb.setEnabled(enable);
        jingleIntervalTf.setEditable(enable);
        if (!enable) {
          protect1stCb.setSelected(false);
          jingleIntervalEnableCb.setSelected(false);
        }

      }
    });

    SelectionInList<WordDistributionStrategy> wordDistSelection = new SelectionInList<WordDistributionStrategy>(WordDistributionStrategy.values(),
        this.model.getBufferedModel("wordDistributionStrategy"));
    JComboBox<WordDistributionStrategy> wordDistCmb = BasicComponentFactory.createComboBox(wordDistSelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = 7985870900294296891L;

      @Override
      @SuppressWarnings("rawtypes")
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        WordDistributionStrategy strategy = (WordDistributionStrategy) value;
        if (strategy == null) {
          strategy = WordDistributionStrategy.RANDOM;
        }
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        String val = strategy.name().toLowerCase();
        setText(textProvider.getString("settings.property.shuffleWordDistribution." + val));
        return this;
      }
    });
    this.add(new JLabel(this.textProvider.getString("settings.property.shuffleWordDistribution")), cc.xy(2, row));
    this.add(wordDistCmb, cc.xywh(4, row, 5, 1));
    row += 2;

    ListCellRenderer<Object> profileIdRenderer = new DefaultListCellRenderer() {
      private static final long serialVersionUID = -3128851951559170210L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c  = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
          setText(textProvider.getString("playlistprofilemanager.ref.none"));
        } else {
          for (PlaylistProfile p : model.getProfiles()) {
            if (p.getId().equals(value)) {
              setText(textProvider.getString("playlistprofilemanager.ref.other", p.getName()));
              break;
            }
          }
        }
        return c;
      }

    };

    SelectionInList<String> trackRuleProfileRefSelection = new SelectionInList<>(model.getProfileRefListModel(), model.getBufferedModel("trackRuleFromProfile"));
    final JComboBox<String> trackRuleRefCmb = BasicComponentFactory.createComboBox(trackRuleProfileRefSelection, profileIdRenderer);
    this.add(new JLabel(this.textProvider.getString("playlistprofilemanager.property.trackRuleRef")), cc.xy(2, row));
    this.add(trackRuleRefCmb, cc.xywh(4, row, 5, 1));
    row += 2;

    SelectionInList<String> artistNormalizationProfileRefSelection = new SelectionInList<>(model.getProfileRefListModel(),
        model.getBufferedModel("artistNormalizationFromProfile"));
    final JComboBox<String> artistNormRefCmb = BasicComponentFactory.createComboBox(artistNormalizationProfileRefSelection, profileIdRenderer);
    this.add(new JLabel(this.textProvider.getString("playlistprofilemanager.property.aritstNormalizationRef")), cc.xy(2, row));
    this.add(artistNormRefCmb, cc.xywh(4, row, 5, 1));
    row += 2;
    


  }

}
