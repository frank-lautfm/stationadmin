/**
 * 
 */
package de.stationadmin.gui.playlist.config.generate;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tag.TagManager;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;

/**
 * @author korf
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class PlaylistGeneratorBaseConfigurationPanel extends JPanel {
  private static final long serialVersionUID = 7684484525261978226L;
  private TagManager titleTagManager;
  private TextProvider textProvider;
  private PlaylistConfigurationModel model;

  boolean updateInProgress = false;

  public PlaylistGeneratorBaseConfigurationPanel(ClientContext ctx, PlaylistConfigurationModel model) {
    this.textProvider = ctx.getTextProvider();
    this.titleTagManager = ctx.getAdminClient().getTagManager();
    this.model = model;
    this.init();
  }

  private void updateSelection(JList list, List<String> tags, List<?> selection) {
    if (!updateInProgress) {
      int[] idxs = new int[selection.size()];
      for (int i = 0; i < selection.size(); i++) {
        idxs[i] = tags.indexOf(selection.get(i));
      }
      list.setSelectedIndices(idxs);
    }

  }

  private void init() {
    this.setLayout(new FormLayout("5dlu,pref,5dlu,170dlu,5dlu", "5dlu,pref,8dlu,50dlu,3dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu,50dlu,5dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    {
      JXLabel infoLabel = new JXLabel(textProvider.getString("playlistcfg.property.generateInfo"));
      infoLabel.setLineWrap(true);
      this.add(infoLabel, cc.xywh(2, row, 3, 1));
      row += 2;
    }

    // generate tags
    {
      final ValueModel tagsModel = this.model.getBufferedModel("generateTags");

      this.add(new JLabel(this.textProvider.getString("playlistcfg.property.generateTags")), cc.xy(2, row, CellConstraints.LEFT, CellConstraints.TOP));
      final List<String> tags = this.titleTagManager.getTags();
      Collections.sort(tags);
      final JList list = new JList(tags.toArray());
      list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

      List<?> selection = (List<?>) tagsModel.getValue();
      updateSelection(list, tags, selection);
      tagsModel.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt.getNewValue() instanceof ArrayList) {
            updateSelection(list, tags, (List<?>) evt.getNewValue());

          } else {
            String titleTagStr = (String) evt.getNewValue();
            ArrayList<String> selection = new ArrayList<String>();
            if (titleTagStr != null) {
              String[] tags = StringUtils.split(titleTagStr, ";");
              for (String tag : tags) {
                selection.add(tag);
              }
            }
            updateSelection(list, tags, (List<?>) selection);
          }
        }
      });

      list.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            updateInProgress = true; // block further UI updates as we are already reactiving to an UI update
            try {
              Object[] values = list.getSelectedValues();
              List<String> tags = new ArrayList<String>();
              for (Object value : values) {
                tags.add((String) value);
              }
              if (tagsModel.getValue() == null || !tagsModel.getValue().equals(tags)) {
                tagsModel.setValue(tags);
              }
            } finally {
              updateInProgress = false;
            }
          }
        }

      });

      this.add(new JScrollPane(list), cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
    }

    {
      this.add(new JLabel(this.textProvider.getString("playlistcfg.property.generateTagsAll")), cc.xy(2, row, CellConstraints.LEFT, CellConstraints.TOP));
      JRadioButton anyBtn = BasicComponentFactory.createRadioButton(model.getBufferedModel("generateTagsAll"), Boolean.FALSE, this.textProvider.getString("playlistcfg.property.generateTagsAll.any"));
      JRadioButton allBtn = BasicComponentFactory.createRadioButton(model.getBufferedModel("generateTagsAll"), Boolean.TRUE, this.textProvider.getString("playlistcfg.property.generateTagsAll.all"));

      JPanel options = new JPanel(new GridLayout(2, 1));
      options.add(anyBtn);
      options.add(allBtn);
      this.add(options, cc.xy(4, row));

      row += 2;
    }

    // generate length
    {
      this.add(new JLabel(this.textProvider.getString("playlistcfg.property.generateLength")), cc.xy(2, row));
      JTextField tf = BasicComponentFactory.createIntegerField(this.model.getBufferedModel("generateLength"));
      tf.setColumns(2);
      JPanel lenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      lenPanel.add(tf);
      lenPanel.add(new JLabel(" Stunden"));
      this.add(lenPanel, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));
      row += 2;
    }

    // max number of titles per artist
    {
      this.add(new JLabel(this.textProvider.getString("playlistcfg.property.generateMaxArtistTitles")), cc.xy(2, row));
      JTextField tf = BasicComponentFactory.createIntegerField(this.model.getBufferedModel("generateMaxArtistTitles"));
      tf.setColumns(2);
      this.add(tf, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));
      row += 2;
    }

    // minimize artist repeats
    {
      this.add(new JLabel(this.textProvider.getString("playlistcfg.property.generateOptions")), cc.xy(2, row));
      JCheckBox cb = BasicComponentFactory.createCheckBox(this.model.getBufferedModel("generateMinimizeArtistRepeats"),
          this.textProvider.getString("playlistcfg.property.generateMinimizeArtistRepeats"));
      this.add(cb, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));
      row += 2;

    }

    // title repeat level
    {
      Integer[] values = new Integer[] { -1, 0, 1, 2, 3 };
      SelectionInList<Integer> titleRepeatSelection = new SelectionInList<Integer>(values, this.model.getBufferedModel("generateTitleRepeatLevel"));
      JComboBox cmb = BasicComponentFactory.createComboBox(titleRepeatSelection, new DefaultListCellRenderer() {
        private static final long serialVersionUID = 4404056309589633917L;

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          setText(textProvider.getString("playlistcfg.property.generateTitleRepeatLevel.option." + value));
          return comp;
        }

      });
      this.add(cmb, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));
      row += 2;
    }

    {
      this.add(new JLabel(this.textProvider.getString("playlistcfg.property.generatePushTag")), cc.xy(2, row));
      row += 2;

      ArrayList<String> values = new ArrayList<String>();
      values.add(null);
      values.addAll(this.titleTagManager.getTags());
      JComboBox tagCombo = new JComboBox(values.toArray());

      JComboBox weightCombo = new JComboBox(new Integer[] { -9, -3, -2, -1, 0, 1, 2, 3 });
      weightCombo.setRenderer(new DefaultListCellRenderer() {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          if (value != null && !value.equals(Integer.valueOf(0))) {
            setText(textProvider.getString("playlistcfg.property.generatePushTag.option." + value));
          } else {
            setText(" ");
          }
          return comp;
        }

      });

      JComboBox maxCombo = new JComboBox(new Float[] { 1f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f });
      maxCombo.setRenderer(new DefaultListCellRenderer() {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          if (value.equals(Float.valueOf(1f))) {
            setText(" ");
          } else {
            int p = (int) (((Float) value).floatValue() * 100);
            setText(p + "%");
          }
          return comp;
        }

      });

      final TableCellRenderer weighRenderer = new DefaultTableCellRenderer() {
        private static final long serialVersionUID = 3085732842546961918L;

        @Override
        protected void setValue(Object value) {
          if (value != null && !value.equals(Integer.valueOf(0))) {
            setText(textProvider.getString("playlistcfg.property.generatePushTag.option." + value));
          } else {
            setText(" ");
          }
        }

      };

      final TableCellRenderer maxRenderer = new DefaultTableCellRenderer() {
        private static final long serialVersionUID = 3085732842546961918L;

        @Override
        protected void setValue(Object value) {
          if (value.equals(Float.valueOf(1f))) {
            setText(" ");
          } else {
            int p = (int) (((Float) value).floatValue() * 100);
            setText(p + "%");
          }
        }

      };

      JXTable table = new JXTable(model.getWeightTableModel()) {
        private static final long serialVersionUID = -2623802397206568002L;

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
          if (column == 1) {
            return weighRenderer;
          }
          if (column == 2) {
            return maxRenderer;
          }
          return super.getCellRenderer(row, column);
        }

      };
      table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(tagCombo));
      table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(weightCombo));
      table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(maxCombo));
      table.getColumnModel().getColumn(2).setMaxWidth(80);
      table.getColumnModel().getColumn(2).setPreferredWidth(80);

      this.add(new JScrollPane(table), cc.xywh(2, row, 3, 1));
      row += 2;

      model.getBufferedModel("generatePushTag").addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          ((PlaylistConfigurationModel.GenerateWeightTableModel) model.getWeightTableModel()).rebuild();
        }
      });

    }

  }

}
