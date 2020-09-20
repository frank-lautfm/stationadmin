package de.stationadmin.gui.playlist.config.shuffle;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXLabel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.scheduled.ScheduledItem;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;

public class ScheduledItemPanel extends JPanel {
  private static final long serialVersionUID = -7863979528920719870L;
  private ClientContext ctx;
  private PlaylistConfigurationModel model;

  private List<ScheduledItemRule> rules = new ArrayList<>();
  private DefaultListModel<ScheduledItemRule> ruleListModel = new DefaultListModel<>();
  private ValueModel selection = new ValueHolder();

  private ValueModel scheduledItem = new ValueHolder(null, true);
  private ValueModel scheduledItemHour = new ValueHolder(-1);
  private ValueModel scheduledItemMinute = new ValueHolder(0);
  private ValueModel scheduledItemInterval = new ValueHolder(0);
  private ValueModel scheduledItemDay = new ValueHolder(-1);

  public ScheduledItemPanel(ClientContext ctx, PlaylistConfigurationModel model) {
    super();
    this.ctx = ctx;
    this.model = model;

    this.initRules();

    model.getBufferedModel("shuffleOpts").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        initRules();
      }
    });

    this.init();
  }

  @SuppressWarnings("unchecked")
  HashMap<String, Object> getOptions() {
    return (HashMap<String, Object>) model.getBufferedModel("shuffleOpts").getValue();
  }

  @SuppressWarnings("unchecked")
  private void initRules() {
    HashMap<String, Object> opts = getOptions();
    List<ScheduledItemRule> rules = new ArrayList<>();
    this.ruleListModel.clear();
    if (opts.containsKey("scheduled")) {
      List<Map<String, Object>> list = (List<Map<String, Object>>) opts.get("scheduled");
      for (Map<String, Object> map : list) {
        if (map.containsKey("id")) {
          ScheduledItem item = this.ctx.getAdminClient().getPlaylistService().getScheduledItem((String) map.get("id"));
          if (item != null) {
            ScheduledItemRule rule = new ScheduledItemRule(item, map);
            rules.add(rule);
            ruleListModel.addElement(rule);
          }
        }
      }
    }
    this.rules = rules;
  }

  private void updateOptions() {
    if (rules.size() > 0) {
      List<Map<String, Object>> list = new ArrayList<>();
      rules.forEach(r -> list.add(r.toMap()));
      getOptions().put("scheduled", list);
    } else {
      getOptions().remove("scheduled");
    }
  }

  @SuppressWarnings("unchecked")
  private JPanel createScheduledItemPanel() {

    JPanel panel = new JPanel(new FormLayout("pref,5dlu,max(pref;120dlu)", "pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();
    int row = 1;

    {
      SelectionInList<ScheduledItem> itemSelectionInList = new SelectionInList<>(ctx.getAdminClient().getPlaylistService().getScheduledItems(), scheduledItem);
      JComboBox<ScheduledItem> itemSelection = BasicComponentFactory.createComboBox(itemSelectionInList);
      panel.add(new JLabel(ctx.getString("playlistcfg.property.scheduleditem.item")), cc.xy(1, 1));
      panel.add(itemSelection, cc.xy(3, row));
      row += 2;
    }
    {
      Integer[] dayOpts = { -1, -2, -3, 1, 2, 3, 4, 5, 6, 0 };
      SelectionInList<Integer> daySelectionInList = new SelectionInList<>(dayOpts, scheduledItemDay);
      JComboBox<Integer> daySelection = BasicComponentFactory.createComboBox(daySelectionInList, new DefaultListCellRenderer() {
        private static final long serialVersionUID = -6540555659669518513L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          setText(ctx.getTextProvider().getString("playlistcfg.property.scheduleditem.day." + value));
          return comp;
        }
        
      });
      panel.add(new JLabel(ctx.getString("playlistcfg.property.scheduleditem.day")), cc.xy(1, row));
      panel.add(daySelection, cc.xy(3, row));

      row += 2;
    }
    {
      List<Integer> hours = new ArrayList<>();
      for (int i = -1; i < 23; i++) {
        hours.add(i);
      }
      SelectionInList<Integer> hourSelectionInList = new SelectionInList<>(hours, scheduledItemHour);
      JComboBox<ScheduledItem> hourSelection = BasicComponentFactory.createComboBox(hourSelectionInList, new DefaultListCellRenderer() {
        private static final long serialVersionUID = -1236436512327071569L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          if (value instanceof Integer && ((Integer) value).intValue() == -1) {
            setText(ctx.getString("playlistcfg.property.scheduleditem.hour.repeat"));
          }
          return comp;
        }

      });
      panel.add(new JLabel(ctx.getString("playlistcfg.property.scheduleditem.hour")), cc.xy(1, row));
      panel.add(hourSelection, cc.xy(3, row));
      row += 2;
    }

    {
      List<Integer> minutes = new ArrayList<>();
      for (int i = 0; i < 60; i++) {
        minutes.add(i);
      }
      SelectionInList<Integer> minuteSelectionInList = new SelectionInList<>(minutes, scheduledItemMinute);
      JComboBox<ScheduledItem> minuteSelection = BasicComponentFactory.createComboBox(minuteSelectionInList);
      panel.add(new JLabel(ctx.getString("playlistcfg.property.scheduleditem.minute")), cc.xy(1, row));
      panel.add(minuteSelection, cc.xy(3, row));
      row += 2;

    }

    {
      List<Integer> intervals = new ArrayList<>();
      intervals.add(-5);
      intervals.add(-10);
      intervals.add(-15);
      intervals.add(-20);
      intervals.add(-30);
      for (int i = 0; i < 13; i++) {
        intervals.add(i);
      }
      SelectionInList<Integer> intervalSelectionInList = new SelectionInList<>(intervals, scheduledItemInterval);
      final JComboBox<ScheduledItem> intervalSelection = BasicComponentFactory.createComboBox(intervalSelectionInList, new DefaultListCellRenderer() {
        private static final long serialVersionUID = -1236436512327071569L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          try {
            int iVal = value instanceof Integer ? ((Integer) value).intValue() : 0;
            if (iVal == 0) {
              setText(ctx.getString("playlistcfg.property.scheduleditem.interval.once"));
            } else if (iVal == 1) {
              setText(ctx.getString("playlistcfg.property.scheduleditem.interval.hour1"));
            } else if (iVal < 0 ) {
              setText(ctx.getString("playlistcfg.property.scheduleditem.interval.minutex", Integer.toString(-iVal)));
            } else {
              setText(ctx.getString("playlistcfg.property.scheduleditem.interval.hourx", Integer.toString(iVal)));

            }
          } catch (Exception e) {
            e.printStackTrace();
          }
          return comp;
        }

      });
      panel.add(new JLabel(ctx.getString("playlistcfg.property.scheduleditem.interval")), cc.xy(1, row));
      panel.add(intervalSelection, cc.xy(3, row));
      row += 2;

      scheduledItemHour.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          int hour = (Integer) evt.getNewValue();
          if (hour > -1) {
            scheduledItemInterval.setValue(0);
            intervalSelection.setEnabled(false);
          } else {
            intervalSelection.setEnabled(true);
          }
        }
      });

    }

    return panel;
  }

  @SuppressWarnings("rawtypes")
  private void init() {
    this.setLayout(new FormLayout("5dlu,min(pref;250dlu):grow,5dlu", "5dlu,pref,8dlu,100dlu,15dlu,pref,5dlu,pref,15dlu,pref,15dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    {
      JXLabel infoLabel = new JXLabel(ctx.getString("playlistcfg.property.scheduleditem"));
      infoLabel.setLineWrap(true);
      this.add(infoLabel, cc.xy(2, row));
      row += 2;
    }

    {
      SelectionInList<ScheduledItemRule> ruleSelection = new SelectionInList<ScheduledItemRule>(ruleListModel, this.selection);
      JList list = BasicComponentFactory.createList(ruleSelection);
      this.add(new JScrollPane(list), cc.xy(2, row));
      row += 2;
    }

    {
      JPanel panel = this.createScheduledItemPanel();
      this.add(panel, cc.xy(2, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
    }

    {
      JPanel buttons = new JPanel(new GridLayout(1, 2, 5, 5));
      buttons.add(new JButton(new AcceptAction()));
      buttons.add(new JButton(new DeleteAction()));
      this.add(buttons, cc.xy(2, row, CellConstraints.CENTER, CellConstraints.CENTER));
      row += 2;
    }

    selection.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        ScheduledItemRule rule = (ScheduledItemRule) evt.getNewValue();
        if (rule != null) {
          scheduledItem.setValue(rule.getScheduledItem());
          scheduledItemDay.setValue(rule.getDay());
          scheduledItemHour.setValue(rule.getHour());
          scheduledItemMinute.setValue(rule.getMinute());
          scheduledItemInterval.setValue(rule.getInterval());
        } else {
          setRuleDefaults();
        }

      }
    });

  }

  private void setRuleDefaults() {
    scheduledItem.setValue(null);
    scheduledItemHour.setValue(-1);
    scheduledItemMinute.setValue(0);
    scheduledItemInterval.setValue(0);
    scheduledItemDay.setValue(-1);
  }

  private class AcceptAction extends AbstractAction {
    private static final long serialVersionUID = -3292475092467146692L;

    public AcceptAction() {
      super(ctx.getString("playlistcfg.action.advice.apply"));
      setEnabled(scheduledItem.getValue() != null);
      scheduledItem.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          setEnabled(scheduledItem.getValue() != null);
        }
      });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      boolean isNew = selection.getValue() == null;
      ScheduledItemRule rule = selection.getValue() != null ? (ScheduledItemRule) selection.getValue() : new ScheduledItemRule();
      rule.setDay((Integer)scheduledItemDay.getValue());
      rule.setScheduledItem((ScheduledItem) scheduledItem.getValue());
      rule.setHour((Integer) scheduledItemHour.getValue());
      rule.setMinute((Integer) scheduledItemMinute.getValue());
      rule.setInterval((Integer) scheduledItemInterval.getValue());
      if (isNew) {
        rules.add(rule);
        ruleListModel.addElement(rule);
      }
      updateOptions();
    }

  }

  private class DeleteAction extends AbstractAction {
    private static final long serialVersionUID = -2109973968542373523L;

    DeleteAction() {
      super(ctx.getTextProvider().getString("playlistcfg.action.advice.delete"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      if (selection.getValue() != null) {
        rules.remove(selection.getValue());
        ruleListModel.removeElement(selection.getValue());
        updateOptions();
      }
      // just clear the panel
      setRuleDefaults();

    }

  }

}
