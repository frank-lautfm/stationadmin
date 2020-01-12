/**
 * 
 */
package de.stationadmin.gui.playlist.config.generate;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXLabel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.shuffle.Advice;
import de.stationadmin.base.playlist.shuffle.TagSequenceAdvice;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;

/**
 * @author korf
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PlaylistGeneratorAdviceConfigurationPanel extends JPanel {
  private static final long serialVersionUID = 5393828754134006427L;
  private TagManager titleTagManager;
  private TextProvider textProvider;
  private PlaylistConfigurationModel model;
  private ValueModel selection = new ValueHolder();

  private ValueModel tagAdvicePatterns = new ValueHolder();
  private ValueModel tagAdviceMode = new ValueHolder(Boolean.FALSE);
  private ValueModel tagAdviceNext = new ValueHolder();

  public PlaylistGeneratorAdviceConfigurationPanel(ClientContext ctx, PlaylistConfigurationModel model) {
    this.textProvider = ctx.getTextProvider();
    this.titleTagManager = ctx.getAdminClient().getTagManager();
    this.model = model;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("5dlu,min(pref;250dlu):grow,5dlu", "5dlu,pref,8dlu,50dlu,15dlu,pref,5dlu,pref,15dlu,pref,15dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    {
      JXLabel infoLabel = new JXLabel(textProvider.getString("playlistcfg.property.generateInfo.advice"));
      infoLabel.setLineWrap(true);
      this.add(infoLabel, cc.xy(2, row));
      row += 2;
    }

    {
      SelectionInList<Advice> adviceSelection = new SelectionInList<Advice>(this.model.getAdvices(), this.selection);
      JList list = BasicComponentFactory.createList(adviceSelection, new AdviceListCellRenderer(this.textProvider));
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

    {
      JPanel panel = new JPanel(new FormLayout("pref,2dlu,pref,2dlu,pref", "pref"));
      panel.add(new JLabel(textProvider.getString("playlistcfg.advice.titlename.description.pre")), cc.xy(1, 1));
      JTextField tf = BasicComponentFactory.createIntegerField(model.getTitleNameAdviceLimit(), 0);
      tf.setColumns(2);
      panel.add(tf, cc.xy(3, 1));
      panel.add(new JLabel(textProvider.getString("playlistcfg.advice.titlename.description.post")), cc.xy(5, 1));

      this.add(panel, cc.xy(2, row, CellConstraints.FILL, CellConstraints.CENTER));
      row += 2;
    }

  }

  private JPanel createTagAdvicePanel() {
    JPanel panel = new JPanel(new FormLayout("75dlu,2dlu,pref:grow", "pref,3dlu,20dlu,3dlu,pref"));
    CellConstraints cc = new CellConstraints();

    List<String> tagList = this.titleTagManager.getTags();
    String[] tags = tagList.toArray(new String[tagList.size()]);

    Boolean[] options = new Boolean[] { Boolean.TRUE, Boolean.FALSE };
    SelectionInList<Boolean> optionSelection = new SelectionInList<Boolean>(options, this.tagAdviceMode);
    JComboBox modeCmb = BasicComponentFactory.createComboBox(optionSelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = 5153263846657730091L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value.equals(Boolean.TRUE)) {
          setText(textProvider.getString("playlistcfg.property.generate.advice.then"));
        } else {
          setText(textProvider.getString("playlistcfg.property.generate.advice.not"));
        }
        return comp;
      }

    });

    TagSequenceEditor pattern = new TagSequenceEditor(tags, this.tagAdvicePatterns, true);
    pattern.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
    TagSequenceEditor next = new TagSequenceEditor(tags, this.tagAdviceNext, false);
    next.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

    JXLabel desc = new JXLabel(this.textProvider.getString("playlistcfg.property.generate.advice.if"));
    desc.setLineWrap(true);
    panel.add(desc, cc.xywh(1, 1, 3, 1));
    panel.add(pattern, cc.xywh(1, 3, 3, 1));
    panel.add(modeCmb, cc.xy(1, 5));
    panel.add(next, cc.xy(3, 5));

    this.selection.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updateAdviceUI();
      }
    });

    return panel;
  }

  private void updateAdviceUI() {
    if (selection.getValue() instanceof TagSequenceAdvice) {
      TagSequenceAdvice rule = (TagSequenceAdvice) selection.getValue();
      tagAdvicePatterns.setValue(rule.getPattern());
      tagAdviceNext.setValue(rule.getNext() != null ? new String[] { rule.getNext() } : null);
      tagAdviceMode.setValue(rule.isNextMustMatch());
    } else {
      tagAdvicePatterns.setValue(null);
      tagAdviceNext.setValue(null);
      tagAdviceMode.setValue(false);
    }

  }

  private List<Advice> getAdvices() {
    return new ArrayList<Advice>((List<Advice>) this.model.getAdvices().getValue());
  }

  private class AcceptAction extends AbstractAction {
    private static final long serialVersionUID = -5124366183683362535L;

    AcceptAction() {
      super(textProvider.getString("playlistcfg.action.advice.apply"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      List<Advice> advices = getAdvices();
      try {
        TagSequenceAdvice rule = (TagSequenceAdvice) selection.getValue();
        String[] pattern = (String[]) tagAdvicePatterns.getValue();
        boolean mustMatch = (Boolean) tagAdviceMode.getValue();
        String[] next = (String[]) tagAdviceNext.getValue();
        if (rule != null) {
          advices.remove(selection.getValue());
        }
        rule = new TagSequenceAdvice(titleTagManager, pattern, mustMatch, next[0]);
        advices.add(rule);

        Collections.sort(advices, new AdviceComparator());

        model.getAdvices().setValue(advices);
        updateAdviceUI();
      } catch (Exception e) {
        e.printStackTrace();
      }

    }

  }

  private class DeleteAction extends AbstractAction {
    private static final long serialVersionUID = -2109973968542373523L;

    DeleteAction() {
      super(textProvider.getString("playlistcfg.action.advice.delete"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      if (selection.getValue() != null) {
        // remove entry from list
        List<Advice> advices = getAdvices();
        advices.remove(selection.getValue());
        model.getAdvices().setValue(advices);
      } else {
        // just clear the panel
        tagAdvicePatterns.setValue(null);
        tagAdviceNext.setValue(null);
        tagAdviceMode.setValue(false);
      }

    }

  }

  private static class AdviceComparator implements Comparator<Advice> {

    @Override
    public int compare(Advice o1, Advice o2) {
      try {
        return o1.toJSON().compareToIgnoreCase(o2.toJSON());
      } catch (Exception e) {
        return 0;
      }
    }

  }

}
