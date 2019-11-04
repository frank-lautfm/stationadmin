package de.stationadmin.gui.playlist.profile;

import java.awt.Component;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.profile.AdTriggerCfg;
import de.stationadmin.base.playlist.shuffle.AdTriggerEngine;
import de.stationadmin.base.playlist.shuffle.AdTriggerEngine.AdJingleCollisionStrategy;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.EnumListCellRenderer;
import de.stationadmin.gui.util.NonObservingPresentationModel;

public class ProfileAdTriggerPanel extends JPanel {
  private static final long serialVersionUID = -1320712762374463028L;
  private ClientContext ctx;
  private PlaylistProfileModel profileModel;
  private NonObservingPresentationModel<AdTriggerCfg> model;
  private ValueModel enabledModel = new ValueHolder(Boolean.FALSE);

  public ProfileAdTriggerPanel(ClientContext ctx, PlaylistProfileModel model) {
    this.ctx = ctx;
    this.profileModel = model;
    this.model = model.getAdTriggerModel();
    this.init();
  }

  @SuppressWarnings("unchecked")
  private void init() {
    StringBuilder rowSpec = new StringBuilder();
    rowSpec.append("5dlu,"); // border
    rowSpec.append("pref,5dlu,"); // enable
    rowSpec.append("pref,5dlu,"); // position 1
    rowSpec.append("pref,5dlu,"); // position 2
    rowSpec.append("pref,5dlu,"); // ad separator
    rowSpec.append("pref,5dlu,"); // ad trigger
    rowSpec.append("pref,5dlu,"); // jingle collision strategy
    rowSpec.append("pref,4dlu:grow,"); // jingle collision strategy
    rowSpec.append("pref,4dlu");

    this.setLayout(new FormLayout("5dlu,pref,5dlu,180dlu,5dlu", rowSpec.toString()));
    CellConstraints cc = new CellConstraints();

    boolean enabled = false;
    enabledModel.setValue(enabled);
    model.getBeanChannel().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        enabledModel.setValue(model.getBean().getPos1() > -1);
      }
    });

    int row = 2;
    JCheckBox enabledCb = BasicComponentFactory.createCheckBox(enabledModel, ctx.getString("settings.adtrigger"));
    this.add(enabledCb, cc.xywh(2, row, 3, 1));
    row += 2;

    final JTextField pos1Tf = BasicComponentFactory.createIntegerField(model.getBufferedModel("pos1"), -1);
    pos1Tf.setColumns(3);
    this.add(new JLabel(ctx.getString("settings.adtrigger.position1")), cc.xy(2, row));
    this.add(pos1Tf, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));
    pos1Tf.setEditable(enabled);
    row += 2;

    final JTextField pos2Tf = BasicComponentFactory.createIntegerField(model.getBufferedModel("pos2"), -1);
    pos2Tf.setColumns(3);
    this.add(new JLabel(ctx.getString("settings.adtrigger.position2")), cc.xy(2, row));
    this.add(pos2Tf, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));
    pos2Tf.setEditable(enabled);
    row += 2;

    SelectionInList<Integer> separatorSelection = new SelectionInList<Integer>(getAdSeparatorOptions(), model.getBufferedModel("seperatorId"));
    final JComboBox<Integer> separatorCmb = BasicComponentFactory.createComboBox(separatorSelection, new JingleRenderer());
    this.add(new JLabel(ctx.getString("settings.adtrigger.separator")), cc.xy(2, row));
    this.add(separatorCmb, cc.xy(4, row));
    separatorCmb.setEnabled(enabled);
    row += 2;

    SelectionInList<Integer> triggerSelection = new SelectionInList<Integer>(getAdTriggerOptions(), model.getBufferedModel("triggerId"));
    final JComboBox<Integer> triggerCmb = BasicComponentFactory.createComboBox(triggerSelection, new JingleRenderer());
    this.add(new JLabel(ctx.getString("settings.adtrigger.trigger")), cc.xy(2, row));
    this.add(triggerCmb, cc.xy(4, row));
    triggerCmb.setEnabled(enabled);
    row += 2;

    SelectionInList<AdJingleCollisionStrategy> jingleStrategySelection = new SelectionInList<AdTriggerEngine.AdJingleCollisionStrategy>(AdJingleCollisionStrategy.values(),
        model.getBufferedModel("jingleCollisionStrategy"));
    final JComboBox<Integer> jingleCmb = BasicComponentFactory.createComboBox(jingleStrategySelection,
        new EnumListCellRenderer(ctx.getTextProvider(), "settings.adtrigger.jingle"));
    this.add(new JLabel(ctx.getString("settings.adtrigger.jingle")), cc.xywh(2, row, 3, 1));
    row += 2;
    this.add(jingleCmb, cc.xywh(2, row, 3, 1));
    jingleCmb.setEnabled(enabled);
    row += 2;

    enabledModel.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        boolean enabled = Boolean.TRUE.equals(evt.getNewValue());
        pos1Tf.setEditable(enabled);
        pos2Tf.setEditable(enabled);
        separatorCmb.setEnabled(enabled);
        triggerCmb.setEnabled(enabled);
        jingleCmb.setEnabled(enabled);

        if (enabled) {
          model.getBufferedModel("pos1").setValue(20);
          model.getBufferedModel("pos2").setValue(50);
        } else {
          model.getBufferedModel("pos1").setValue(-1);
        }

      }
    });

    model.getBufferedModel("pos1").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        int pos1 = (Integer) evt.getNewValue();
        int pos2 = (Integer) model.getBufferedModel("pos2").getValue();

        if (pos1 > 30) {
          model.getBufferedModel("pos1").setValue(30);
          pos1 = 30;
          if (!profileModel.isInitializing()) {
            Toolkit.getDefaultToolkit().beep();
          }
        }

        if (pos2 - pos1 < 20) {
          model.getBufferedModel("pos2").setValue(pos1 + 20);
          if (!profileModel.isInitializing()) {
            Toolkit.getDefaultToolkit().beep();
          }
        } else if (pos2 - pos1 > 40) {
          model.getBufferedModel("pos2").setValue(Math.min(59, pos1 + 40));
          if (!profileModel.isInitializing()) {
            Toolkit.getDefaultToolkit().beep();
          }
        }
      }
    });

    model.getBufferedModel("pos2").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        int pos2 = (Integer) evt.getNewValue();
        int pos1 = (Integer) model.getBufferedModel("pos1").getValue();

        if (pos2 < 30) {
          model.getBufferedModel("pos2").setValue(30);
          pos2 = 30;
          if (!profileModel.isInitializing()) {
            Toolkit.getDefaultToolkit().beep();
          }
        }

        if (pos2 > 59) {
          model.getBufferedModel("pos2").setValue(59);
          pos2 = 59;
          if (!profileModel.isInitializing()) {
            Toolkit.getDefaultToolkit().beep();
          }
        }

        if (pos2 - pos1 < 20) {
          model.getBufferedModel("pos1").setValue(pos2 - 20);
          if (!profileModel.isInitializing()) {
            Toolkit.getDefaultToolkit().beep();
          }
        } else if (pos2 - pos1 > 40) {
          model.getBufferedModel("pos1").setValue(Math.min(30, pos2 - 40));
          if (!profileModel.isInitializing()) {
            Toolkit.getDefaultToolkit().beep();
          }
        }
      }
    });

  }

  private List<Integer> getAdSeparatorOptions() {
    ArrayList<BasicTrack> jingles = new ArrayList<BasicTrack>();
    for (BasicTrack track : ctx.getAdminClient().getTrackService().getTrackRegistry().getAllTracks()) {
      if (track.getType() == BasicTrack.TYPE_JINGLE && !(track.getArtist().equals("START_AD_BREAK") || track.getTitle().equals("START_AD_BREAK"))) {
        jingles.add(track);
      }
    }
    Collections.sort(jingles, new Comparator<BasicTrack>() {

      @Override
      public int compare(BasicTrack o1, BasicTrack o2) {
        int result = o1.getArtist().compareTo(o2.getArtist());
        if (result == 0) {
          result = o1.getTitle().compareTo(o2.getTitle());
        }
        return result;
      }
    });

    List<Integer> ids = new ArrayList<Integer>();
    for (BasicTrack track : jingles) {
      ids.add(track.getId());
    }

    ids.add(0, -1);

    return ids;

  }

  private List<Integer> getAdTriggerOptions() {
    ArrayList<Integer> triggers = new ArrayList<Integer>();
    triggers.add(TrackRegistry.STANDARD_AD_TRIGGER_ID);

    for (BasicTrack track : ctx.getAdminClient().getTrackService().getTrackRegistry().getAllTracks()) {
      if (track.getType() == BasicTrack.TYPE_JINGLE && track.getId() > 0 && (track.getArtist().equals("START_AD_BREAK") || track.getTitle().equals("START_AD_BREAK"))) {
        triggers.add(track.getId());
      }
    }

    return triggers;

  }

  private class JingleRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = -7293913840112084061L;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value instanceof Integer) {
        int id = (Integer) value;
        if (id == -1) {
          setText("kein Werbetrenner");
        } else if (id == 0) {
          setText("Standard");
        } else {
          try {
            DetailedTrack track = ctx.getAdminClient().getTrackService().getTrack(id);
            if (track != null) {
              setText(track.toString());
            }
          } catch (Exception e) {

          }
        }
      }
      return this;
    }

  }
}
