package de.stationadmin.gui.tag;

import java.awt.Component;
import java.awt.Dimension;

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

import de.stationadmin.base.tag.TagSet;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.NonObservingPresentationModel;

public class TagSetEditor extends JPanel {
  private static final long serialVersionUID = -783257611910028224L;
  private ClientContext ctx;
  private PresentationModel<TagSet> model = new NonObservingPresentationModel<TagSet>(
      (TagSet) null);

  public TagSetEditor(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.init();
  }

  protected PresentationModel<TagSet> getModel() {
    return model;
  }

  @SuppressWarnings(value = { "unchecked", "rawtypes" })
  private void init() {
    this.setLayout(new FormLayout("3dlu,max(pref;50dlu),3dlu,pref:grow",
        "3dlu,pref,5dlu,pref:grow,3dlu"));
    CellConstraints cc = new CellConstraints();

    JTextField tf = BasicComponentFactory.createTextField(model
        .getBufferedModel("name"));

    this.add(new JLabel(ctx.getString("titletagmanager.property.name")),
        cc.xy(2, 2));
    this.add(tf, cc.xy(4, 2));

    final TableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
      private static final long serialVersionUID = 3085732842546961918L;

      @Override
      protected void setValue(Object value) {
        if (value != null) {
          setText(ctx.getTextProvider().getString("tagset.property.status.option." + value.toString().toLowerCase()));
        } else {
          setText(" ");
        }
      }

    };

    final JXTable table = new JXTable(new TagSetTableModel(ctx, this.model)) {
      private static final long serialVersionUID = 8733983603052232721L;
      
      @Override
      public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == 1) {
          return statusRenderer;
        }
        return super.getCellRenderer(row, column);
      }
    };
    
    JComboBox statusCombo = new JComboBox(new Boolean[] {null, Boolean.TRUE, Boolean.FALSE });
    statusCombo.setRenderer(new DefaultListCellRenderer() {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
          boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null) {
          setText(ctx.getTextProvider().getString("tagset.property.status.option." + value.toString().toLowerCase()));
        } else {
          setText(" ");
        }
        return comp;
      }

    });
    table.getColumn(1).setCellEditor(new DefaultCellEditor(statusCombo));

    
    JScrollPane tableScroll = new JScrollPane(table);
    tableScroll.setPreferredSize(new Dimension(100, 50));
    
    this.add(tableScroll, cc.xywh(2, 4, 3, 1, CellConstraints.FILL, CellConstraints.FILL));

  }
}
