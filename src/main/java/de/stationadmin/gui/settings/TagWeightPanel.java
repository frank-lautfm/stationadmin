/**
 * 
 */
package de.stationadmin.gui.settings;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.PresentationModel;
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
  private PresentationModel<Settings> model;

  /**
   * @param textProvider
   * @param settingsModel
   */
  public TagWeightPanel(ClientContext ctx, PresentationModel<Settings> settingsModel) {
    super();
    this.ctx = ctx;
    this.model = settingsModel;
    this.init();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void init() {
    this.setLayout(new FormLayout("100dlu:grow", "60dlu:grow"));

    ArrayList<TagWeight> list = new ArrayList<TagWeight>(model.getBean().getGenerateGlobalTagWeights());
    TagWeightTableModel tableModel = new TagWeightTableModel(list);
    model.getBufferedModel("generateGlobalTagWeights").setValue(list);

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

    this.add(new JScrollPane(table), new CellConstraints(1, 1));

  }

}
