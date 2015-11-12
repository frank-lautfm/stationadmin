/**
 * 
 */
package de.stationadmin.gui.settings;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.Settings;
import de.stationadmin.gui.ClientContext;


/**
 * @author korf
 * 
 */
public class ArtistLimitPanel extends JPanel {
  private static final long serialVersionUID = -7632838693437186248L;

  private ClientContext ctx;
  private PresentationModel<Settings> model;

  /**
   * @param ctx
   * @param model
   */
  public ArtistLimitPanel(ClientContext ctx, PresentationModel<Settings> model) {
    super();
    this.ctx = ctx;
    this.model = model;
    this.init();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void init() {
    this.setLayout(new FormLayout("100dlu:grow", "pref,5dlu,pref,5dlu,60dlu:grow,8dlu,pref,5dlu,60dlu:grow"));
    CellConstraints cc = new CellConstraints();

    final ArtistLimitTableModel tableModel = new ArtistLimitTableModel(ctx.getTextProvider(), new HashMap<String, Integer>(this.model.getBean()
        .getGenerateArtistPreselectLimits()));

    model.getBufferedModel("generateArtistPreselectLimits").setValue(tableModel.getArtistLimits());

    JPanel preselectProperyPanel = new JPanel(new FormLayout("pref,2dlu,pref,2dlu,pref,8dlu:grow:pref", "pref"));
    JTextField preselectLimitTf = BasicComponentFactory.createIntegerField(tableModel.getDefaultLimit(), 0);
    preselectLimitTf.setColumns(2);
    preselectProperyPanel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.generatePreselect.prefix")), cc.xy(1, 1));
    preselectProperyPanel.add(preselectLimitTf, cc.xy(3, 1));
    preselectProperyPanel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.generatePreselect.suffix")), cc.xy(5, 1));

    this.add(preselectProperyPanel, cc.xy(1, 1));

    this.add(new JLabel("Individuelle Limits für einzelne Künstler"), cc.xy(1, 3)); // FIXME
    final JXTable table = new JXTable(tableModel);
    table.getColumnModel().getColumn(1).setMaxWidth(80);
    table.getColumnModel().getColumn(1).setPreferredWidth(80);

    final TableCellRenderer limitRenderer = new DefaultTableCellRenderer() {
      private static final long serialVersionUID = 3085732842546961918L;

      @Override
      protected void setValue(Object value) {
        this.setHorizontalAlignment(JLabel.RIGHT);
        if (value != null && !value.equals(Integer.valueOf(0))) {
          setText(value.toString());
        } else {
          setText("");
        }
      }
    };
    table.getColumn(1).setCellRenderer(limitRenderer);

    table.setEnabled(tableModel.getDefaultLimit().intValue() > 0);

    this.add(new JScrollPane(table), cc.xy(1, 5));

    
    // tag weights for artist preselection
    
    HashMap<String, Integer> map = new HashMap<String, Integer>(model.getBean().getGenerateArtistPreselectTagWeights());
    TagWeightTableModel tagWeightTableModel = new TagWeightTableModel(map);
    model.getBufferedModel("generateArtistPreselectTagWeights").setValue(map);

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

    final JXTable tagWeightTable = new JXTable(tagWeightTableModel) {
      private static final long serialVersionUID = -2623802397206568002L;

      @Override
      public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == 1) {
          return weighRenderer;
        }
        return super.getCellRenderer(row, column);
      }

    };
    tagWeightTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(tagCombo));
    tagWeightTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(weightCombo));
    tagWeightTable.setEnabled(tableModel.getDefaultLimit().intValue() > 0);

    this.add(new JLabel("Sollen Titel bei einer Vorauswahl bevorzugt werden?"), cc.xy(1, 7));
    this.add(new JScrollPane(tagWeightTable), cc.xy(1, 9));

    tableModel.getDefaultLimit().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        table.setEnabled(tableModel.getDefaultLimit().intValue() > 0);
        tagWeightTable.setEnabled(tableModel.getDefaultLimit().intValue() > 0);
      }
    });

  }
}
