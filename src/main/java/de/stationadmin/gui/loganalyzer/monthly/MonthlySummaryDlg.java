package de.stationadmin.gui.loganalyzer.monthly;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.loganalyzer.MonthlySummary;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;

public class MonthlySummaryDlg extends StationAdminFrame {
  private static final long serialVersionUID = -7654120068678120944L;
  private ValueHolder diffYear = new ValueHolder("--");
  private ValueHolder diffMonth = new ValueHolder("--");

  public MonthlySummaryDlg(ClientContext ctx, List<MonthlySummary> data) {
    super(ctx, "monthlysummaries");
    this.init(data);
  }

  private void init(List<MonthlySummary> data) {
    this.setTitle(this.ctx.getTextProvider().getString("monthlysummaries.dlg.title"));

    JTabbedPane tabPane = new JTabbedPane();
    tabPane.addTab(this.ctx.getTextProvider().getString("monthlysummaries.dlg.tab.tlh"), initTab(data, MonthlySummaryTableModel.TYPE_TLH));
    tabPane.addTab(this.ctx.getTextProvider().getString("monthlysummaries.dlg.tab.uniqs"), initTab(data, MonthlySummaryTableModel.TYPE_UNIQS));

    this.setLayout(new BorderLayout());
    this.add(tabPane, BorderLayout.CENTER);

  }

  private JPanel initTab(List<MonthlySummary> data, int type) {
    JPanel panel = new JPanel(new FormLayout("5dlu,250dlu:grow,5dlu", "5dlu,150dlu:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    final MonthlySummaryTableModel tableModel = new MonthlySummaryTableModel(ctx.getTextProvider(), data, type);
    final JXTable table = new JXTable(tableModel);
    table.setCellSelectionEnabled(true);

    table.addHighlighter(new AbstractHighlighter() {

      @Override
      protected Component doHighlight(Component comp, ComponentAdapter adapter) {
        if (table.convertColumnIndexToModel(adapter.column) == 0) {
          comp.setBackground(new Color(210, 210, 210));
        }
        return comp;
      }
    });

    panel.add(new JScrollPane(table), cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    table.getColumnModel().getColumn(0).setMinWidth(80);
    table.getColumnModel().getColumn(0).setMaxWidth(80);
    for (int i = 1; i < tableModel.getColumnCount(); i++) {
      table.getColumnModel().getColumn(i).setMinWidth(60);
      table.getColumnModel().getColumn(i).setMaxWidth(60);
    }

    final ListSelectionListener statsUpdater = new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        Object value = col > 0 ? tableModel.getValueAt(row, col) : null;
        if (value == null || !(value instanceof Integer)) {
          diffMonth.setValue("--");
          diffYear.setValue("--");
          return;
        }
        int current = (Integer) value;
        NumberFormat fmt = NumberFormat.getInstance();
        fmt.setMaximumFractionDigits(1);

        // previous month
        if (row < 12) {
          Object pValue = row > 0 ? tableModel.getValueAt(row - 1, col) : tableModel.getValueAt(11, col + 1);
          if (pValue instanceof Integer) {
            int prevMonth = (Integer) pValue;
            float diff = 100 * (((float) current / (float) prevMonth) - 1);
            diffMonth.setValue(fmt.format(diff) + "%");
          } else {
            diffMonth.setValue("--");
          }
        } else {
          diffMonth.setValue("--");
        }

        // previous year
        Object pValue = col + 1 < tableModel.getColumnCount() ? tableModel.getValueAt(row, col + 1) : null;
        if (pValue != null && pValue instanceof Integer) {
          int prevYear = (Integer) pValue;
          float diff = 100 * (((float) current / (float) prevYear) - 1);
          diffYear.setValue(fmt.format(diff) + "%");
        } else {
          diffYear.setValue("");
        }

      }
    };

    table.getSelectionModel().addListSelectionListener(statsUpdater);
    table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {

      @Override
      public void columnSelectionChanged(ListSelectionEvent e) {
        statsUpdater.valueChanged(null);
      }

      @Override
      public void columnRemoved(TableColumnModelEvent e) {

      }

      @Override
      public void columnMoved(TableColumnModelEvent e) {

      }

      @Override
      public void columnMarginChanged(ChangeEvent e) {

      }

      @Override
      public void columnAdded(TableColumnModelEvent e) {

      }
    });

    JPanel stats = new JPanel(new FormLayout("pref,5dlu,max(pref;20dlu),8dlu,pref,5dlu,max(pref;20dlu)", "pref"));
    stats.add(new JLabel(ctx.getString("monthlysummaries.diff.year")), cc.xy(1, 1));
    stats.add(BasicComponentFactory.createLabel(diffYear), cc.xy(3, 1, CellConstraints.RIGHT, CellConstraints.CENTER));

    stats.add(new JLabel(ctx.getString("monthlysummaries.diff.month")), cc.xy(5, 1));
    stats.add(BasicComponentFactory.createLabel(diffMonth), cc.xy(7, 1, CellConstraints.RIGHT, CellConstraints.CENTER));

    panel.add(stats, cc.xy(2, 4));

    return panel;

  }

}
