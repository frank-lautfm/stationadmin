package de.stationadmin.gui.playlist.config.shuffle;

import java.awt.Color;
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
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;
import de.stationadmin.gui.playlist.config.generate.TagSequenceEditor;

public class TagSequenceRuleEditor extends JPanel {
  private static final long serialVersionUID = -8787295182939676552L;
  private ClientContext ctx;
  private PlaylistConfigurationModel model;

  private ValueModel selection = new ValueHolder();

  private ValueModel tagAdvicePatterns = new ValueHolder();
  private ValueModel tagAdviceMode = new ValueHolder(Boolean.FALSE);
  private ValueModel tagAdviceNext = new ValueHolder();

  private List<TagSequenceRule> rules = new ArrayList<>();
  private DefaultListModel<TagSequenceRule> ruleListModel = new DefaultListModel<>();

  public TagSequenceRuleEditor(ClientContext ctx, PlaylistConfigurationModel model) {
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
    List<TagSequenceRule> rules = new ArrayList<>();
    this.ruleListModel.clear();
    if (opts.containsKey("tagSequences")) {
      List<Map<String, Object>> list = (List<Map<String, Object>>) opts.get("tagSequences");
      for (Map<String, Object> map : list) {
        TagSequenceRule rule = new TagSequenceRule(map);
        rules.add(rule);
        ruleListModel.addElement(rule);
      }
    }
    this.rules = rules;
  }

  @SuppressWarnings("rawtypes")
  private void init() {
    this.setLayout(new FormLayout("5dlu,min(pref;250dlu):grow,5dlu", "5dlu,pref,8dlu,50dlu,15dlu,pref,5dlu,pref,15dlu,pref,15dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    {
      JXLabel infoLabel = new JXLabel(ctx.getString("playlistcfg.property.tagsequence"));
      infoLabel.setLineWrap(true);
      this.add(infoLabel, cc.xy(2, row));
      row += 2;
    }

    {
      SelectionInList<TagSequenceRule> adviceSelection = new SelectionInList<TagSequenceRule>(ruleListModel, this.selection);
      JList list = BasicComponentFactory.createList(adviceSelection, new TagSequenceRuleRenderer(ctx.getTextProvider()));
      this.add(new JScrollPane(list), cc.xy(2, row));
      row += 2;
    }

    {
      JPanel panel = this.createTagAdvicePanel();
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

  }

  @SuppressWarnings("rawtypes")
  private JPanel createTagAdvicePanel() {
    JPanel panel = new JPanel(new FormLayout("75dlu,2dlu,pref:grow", "pref,3dlu,20dlu,3dlu,pref"));
    CellConstraints cc = new CellConstraints();

    List<String> tagList = ctx.getAdminClient().getTagManager().getTags();
    String[] tags = tagList.toArray(new String[tagList.size()]);

    Boolean[] options = new Boolean[] { Boolean.TRUE, Boolean.FALSE };
    SelectionInList<Boolean> optionSelection = new SelectionInList<Boolean>(options, this.tagAdviceMode);
    JComboBox modeCmb = BasicComponentFactory.createComboBox(optionSelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = 5153263846657730091L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value.equals(Boolean.TRUE)) {
          setText(ctx.getString("playlistcfg.property.generate.advice.then"));
        } else {
          setText(ctx.getString("playlistcfg.property.generate.advice.not"));
        }
        return comp;
      }

    });

    TagSequenceEditor pattern = new TagSequenceEditor(tags, this.tagAdvicePatterns, true);
    pattern.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
    TagSequenceEditor next = new TagSequenceEditor(tags, this.tagAdviceNext, false);
    next.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

    JXLabel desc = new JXLabel(ctx.getString("playlistcfg.property.generate.advice.if"));
    desc.setLineWrap(true);
    panel.add(desc, cc.xywh(1, 1, 3, 1));
    panel.add(pattern, cc.xywh(1, 3, 3, 1));
    panel.add(modeCmb, cc.xy(1, 5));
    panel.add(next, cc.xy(3, 5));

    this.selection.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updateRuleUI();
      }
    });

    return panel;
  }

  private void updateRuleUI() {
    if (selection.getValue() instanceof TagSequenceRule) {
      TagSequenceRule rule = (TagSequenceRule) selection.getValue();
      tagAdvicePatterns.setValue(rule.getPattern());
      tagAdviceNext.setValue(rule.getNext() != null ? new String[] { rule.getNext() } : null);
      tagAdviceMode.setValue(!rule.isNot());
    } else {
      tagAdvicePatterns.setValue(null);
      tagAdviceNext.setValue(null);
      tagAdviceMode.setValue(false);
    }

  }

  private class AcceptAction extends AbstractAction {
    private static final long serialVersionUID = -5124366183683362535L;

    AcceptAction() {
      super(ctx.getString("playlistcfg.action.advice.apply"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      try {
        TagSequenceRule rule = (TagSequenceRule) selection.getValue();
        String[] pattern = (String[]) tagAdvicePatterns.getValue();
        boolean mustMatch = (Boolean) tagAdviceMode.getValue();
        String[] next = (String[]) tagAdviceNext.getValue();
        if (rule != null) {
          rule.setPattern(pattern);
          rule.setNot(!mustMatch);
          rule.setNext(next[0]);
        } else {
          rule = new TagSequenceRule(pattern, !mustMatch, next[0]);
          rules.add(rule);
          ruleListModel.addElement(rule);
        }

        // Collections.sort(advices, new AdviceComparator());
        updateOptions();
        updateRuleUI();
      } catch (Exception e) {
        e.printStackTrace();
      }

    }

  }

  private void updateOptions() {
    List<Map<String, Object>> list = new ArrayList<>();
    rules.forEach(r -> list.add(r.toMap()));
    getOptions().put("tagSequences", list);

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
      } else {
        // just clear the panel
        tagAdvicePatterns.setValue(null);
        tagAdviceNext.setValue(null);
        tagAdviceMode.setValue(false);
      }

    }

  }

}
