/**
 * 
 */
package de.stationadmin.gui.loganalyzer.dailysummary;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.loganalyzer.DailySummary;
import de.stationadmin.base.loganalyzer.DailySummaryStatistics;
import de.stationadmin.base.loganalyzer.LogAnalyzerService;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.loganalyzer.dailysummary.DailySummaryTableModel.Column;
import de.stationadmin.gui.loganalyzer.util.PropertyPanel;
import de.stationadmin.gui.loganalyzer.util.SetTimeAction;
import de.stationadmin.gui.loganalyzer.util.TimeEditor;
import de.stationadmin.gui.playlist.PopupListener;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.DateTableCellRenderer;
import de.stationadmin.gui.util.IntTableCellRenderer;
import de.stationadmin.gui.util.TableExportUtils;

/**
 * @author korf
 * 
 */
public class DailySummariesDlg extends StationAdminFrame {
  private static final long serialVersionUID = -6578339840160888655L;
  private TimeEditor fromTime, toTime;
  private ValueModel entriesHolder = new ValueHolder();
  private DailySummaryStatistics statistics = new DailySummaryStatistics();

  /**
   * @param ctx
   * @throws HeadlessException
   */
  public DailySummariesDlg(ClientContext ctx) throws HeadlessException {
    super(ctx, "dailysummaries");
    this.init();
  }

  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(550, 400);
  }

  private void init() {
    this.setTitle(this.ctx.getTextProvider().getString("dailysummaries.dlg.title"));
    this.setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,50dlu:grow,3dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    this.add(createFilterPanel(), cc.xy(2, 2));

    final DailySummaryTableModel tableModel = new DailySummaryTableModel(this.ctx.getTextProvider(), this.entriesHolder);
    final JXTable table = new JXTable(tableModel);
    table.getColumn(Column.DAY.ordinal()).setCellRenderer(
        new DateTableCellRenderer(new SimpleDateFormat(ctx.getTextProvider().getString("extDateFormat"))));
    table.getColumn(Column.LISTENERS.ordinal()).setCellRenderer(new IntTableCellRenderer(0));
    table.getColumn(Column.DURATION.ordinal()).setCellRenderer(
        new IntTableCellRenderer(0, " " + ctx.getTextProvider().getString("dailysummaries.column.duration.unit")));
    table.getColumn(Column.AVG_LISTENING_TIME.ordinal()).setCellRenderer(new IntTableCellRenderer(0));
    table.getColumn(Column.UNIQS.ordinal()).setCellRenderer(new IntTableCellRenderer(-1));

    table.addHighlighter(new AbstractHighlighter() {

      @Override
      protected Component doHighlight(Component comp, ComponentAdapter adapter) {
        int row = table.convertRowIndexToModel(adapter.row);
        int col = table.convertColumnIndexToModel(adapter.column);
        if (col == Column.DURATION.ordinal() && tableModel.get(row).isEstimated()) {
          comp.setFont(ComponentFactory.italicLabelFont);
        }
        return comp;
      }
    });

    final JPopupMenu popup = new JPopupMenu();
    popup.add(TableExportUtils.getCopyToClipboardAction(table, ctx.getTextProvider()));
    popup.add(TableExportUtils.getExportToExcelAction(table, ctx.getTextProvider(), ctx.getTextProvider().getString("dailysummaries.dlg.title")));

    table.addMouseListener(new PopupListener(table, popup));

    this.add(new JScrollPane(table), cc.xy(2, 4));
    this.add(this.createStatusBar(), cc.xy(2, 6));

    this.update();

  }

  private JComponent createStatusBar() {
    final JXStatusBar statusBar = new JXStatusBar();
    statusBar.setOpaque(false);

    PresentationModel<DailySummaryStatistics> model = new PresentationModel<DailySummaryStatistics>(this.statistics);
    final ValueHolder durationHours = new ValueHolder(0);
    model.getModel("duration").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        durationHours.setValue(((Integer) evt.getNewValue()).intValue() / 60);
      }
    });

    JXStatusBar.Constraint durConst = new JXStatusBar.Constraint(new Insets(0, 2, 0, 2));
    PropertyPanel dur = new PropertyPanel(ctx.getTextProvider().getString("dailysummaries.column.duration") + ":", durationHours);
    statusBar.add(dur, durConst);

    JXStatusBar.Constraint listenersConst = new JXStatusBar.Constraint(new Insets(0, 2, 0, 2));
    PropertyPanel listeners = new PropertyPanel(ctx.getTextProvider().getString("dailysummaries.column.listeners") + ":", model.getModel("listeners"));
    statusBar.add(listeners, listenersConst);

    JXStatusBar.Constraint uniqsConst = new JXStatusBar.Constraint(new Insets(0, 2, 0, 2));
    PropertyPanel uniqs = new PropertyPanel(ctx.getTextProvider().getString("dailysummaries.column.uniqs") + ":", model.getModel("uniqs"));
    statusBar.add(uniqs, uniqsConst);

    JXStatusBar.Constraint avgConst = new JXStatusBar.Constraint(new Insets(0, 2, 0, 2));
    PropertyPanel avg = new PropertyPanel(ctx.getTextProvider().getString("dailysummaries.column.avg_listening_time") + ":",
        model.getModel("avgListeningTime"));
    statusBar.add(avg, avgConst);

    return statusBar;
  }

  private JPanel createFilterPanel() {
    JPanel panel = new JPanel(new FormLayout("pref,2dlu,pref,2dlu,pref,2dlu,pref,5dlu:grow,pref", "pref"));
    CellConstraints cc = new CellConstraints();

    this.fromTime = new TimeEditor(this.ctx.getTextProvider(), new Date(System.currentTimeMillis() - LogAnalyzerService.DAY_IN_MS * 7));
    this.fromTime.getTimePanel().setText("0:00");
    this.toTime = new TimeEditor(this.ctx.getTextProvider(), new Date(System.currentTimeMillis()));
    this.toTime.getTimePanel().setText("0:00");

    panel.add(fromTime.getDateChooser(), cc.xy(1, 1));
    panel.add(new JLabel("-"), cc.xy(3, 1));
    panel.add(toTime.getDateChooser(), cc.xy(5, 1));

    final JPopupMenu menu = new JPopupMenu();
    menu.add(new JMenuItem(new SetTimeAction(this.ctx.getTextProvider(), this.fromTime, this.toTime, 0)));
    menu.add(new JMenuItem(new SetTimeAction(this.ctx.getTextProvider(), this.fromTime, this.toTime, 1)));
    menu.add(new JMenuItem(new SetTimeAction(this.ctx.getTextProvider(), this.fromTime, this.toTime, 2)));

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    final JButton settingsBtn = new JButton(AppUtils.getIcon("arrowdown.png"));
    settingsBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        menu.show(settingsBtn, settingsBtn.getWidth() - (int) menu.getPreferredSize().getWidth(), settingsBtn.getHeight());

      }
    });
    toolbar.add(settingsBtn);
    panel.add(toolbar, cc.xy(7, 1, CellConstraints.FILL, CellConstraints.FILL));

    JButton filterBtn = new JButton(AppUtils.getIcon("filter.png"));
    filterBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent evt) {
        try {
          update();
        } catch (Exception e) {
          JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "listeners.update.failed"));
        }
      }
    });

    panel.add(filterBtn, cc.xy(9, 1));

    return panel;
  }

  void update() {
    try {
      List<DailySummary> entries = this.ctx.getAdminClient().getLogAnalyzerService()
          .getDailySummaries(this.fromTime.getDate(), this.toTime.getDate());
      entriesHolder.setValue(entries);
      this.statistics.update(entries);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}
