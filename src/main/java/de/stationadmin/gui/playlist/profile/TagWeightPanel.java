/**
 * 
 */
package de.stationadmin.gui.playlist.profile;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.Settings;
import de.stationadmin.base.playlist.shuffle.TagWeight;
import de.stationadmin.gui.ClientContext;

/**
 * @author korf
 * 
 */
public class TagWeightPanel extends JPanel {
  private static final long serialVersionUID = -2091070394350673287L;
  private ClientContext ctx;
  private PlaylistProfileModel model;

  /**
   * @param textProvider
   * @param settingsModel
   */
  public TagWeightPanel(ClientContext ctx, PlaylistProfileModel model) {
    super();
    this.ctx = ctx;
    this.model = model;
    this.init();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void init() {
    this.setLayout(new FormLayout("5dlu,100dlu:grow,5dlu", "5dlu,pref,5dlu,pref,10dlu,pref,5dlu,60dlu:grow,5dlu"));
    CellConstraints cc = new CellConstraints();

    final ValueModel minRandomValueModel = model.getBufferedModel("tagWeightBottom");
    final JSlider minRandomValueSlider = new JSlider(0, 500);
    minRandomValueSlider.setMinorTickSpacing(25);
    minRandomValueSlider.setMajorTickSpacing(100);
    minRandomValueSlider.setSnapToTicks(true);
    minRandomValueSlider.setPaintTicks(true);
    minRandomValueSlider.addChangeListener(new ChangeListener() {

      @Override
      public void stateChanged(ChangeEvent e) {
        int value = minRandomValueSlider.getValue();
        minRandomValueModel.setValue(value);
      }
    });

    this.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.generateMinRandomValue")), cc.xy(2, 2));
    this.add(minRandomValueSlider, cc.xy(2, 4));

    this.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.generateWeightTags")), cc.xy(2, 6));

    final TagWeightTableModel tableModel = new TagWeightTableModel();

    model.getBeanChannel().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (model.getBean() != null) {
          minRandomValueSlider.setValue(model.getBean().getTagWeightBottom());
          if (model.getBean().getTagWeights() != null) {
            ArrayList<TagWeight> list = new ArrayList(model.getBean().getTagWeights());
            model.getBufferedModel("tagWeights").setValue(list);
            tableModel.setTagWeights(list);
          }
        }
      }
    });

    ArrayList<String> values = new ArrayList<String>();
    values.add(null);
    values.addAll(ctx.getAdminClient().getTagManager().getTags());
    JComboBox tagCombo = new JComboBox(values.toArray());

    JComboBox weightCombo = new JComboBox(new Integer[] { -9, -3, -2, -1, 0, 1, 2, 3 });
    weightCombo.setRenderer(new DefaultListCellRenderer() {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null && !value.equals(Integer.valueOf(0))) {
          setText(ctx.getTextProvider().getString("playlistcfg.property.generatePushTag.option." + value));
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
          setText(ctx.getTextProvider().getString("playlistcfg.property.generatePushTag.option." + value));
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

    JXTable table = new JXTable(tableModel) {
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

    this.add(new JScrollPane(table), new CellConstraints(2, 8));

  }

}
