/**
 * 
 */
package de.stationadmin.gui.loganalyzer.listeners;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.loganalyzer.ListenersEntry;
import de.stationadmin.base.loganalyzer.ListenersStatistics;
import de.stationadmin.base.loganalyzer.LogAnalyzerService;
import de.stationadmin.base.schedule.Schedule.Weekday;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.loganalyzer.listeners.ListenersTableModel.Column;
import de.stationadmin.gui.loganalyzer.util.PropertyPanel;
import de.stationadmin.gui.loganalyzer.util.SetTimeAction;
import de.stationadmin.gui.loganalyzer.util.TimeEditor;
import de.stationadmin.gui.playlist.PopupListener;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.DateTableCellRenderer;
import de.stationadmin.gui.util.IntTableCellRenderer;
import de.stationadmin.gui.util.SwingTools;
import de.stationadmin.gui.util.TableExportUtils;

/**
 * @author korf
 * 
 */
public class ListenersAnalyzer extends StationAdminFrame {
  private static final long serialVersionUID = -7409448305205333449L;

  private TimeEditor fromTime, toTime;
  private ValueModel entriesHolder = new ValueHolder();
  private ValueModel entriesAvgHolder = new ValueHolder();
  private ListenersChart listenersChart;
  private ListenersStatistics stats = new ListenersStatistics();
  private ValueHolder granularity = new ValueHolder(1);
  private ValueHolder weekdays = new ValueHolder(0);

  /**
   * @param ctx
   * @throws HeadlessException
   */
  public ListenersAnalyzer(ClientContext ctx) {
    super(ctx, "listeners.analyzer");

    this.init();
  }

  private void init() {
    this.setTitle(this.ctx.getTextProvider().getString("listenersanalyzer.dlg.title"));
    this.setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref:grow,5dlu"));
    CellConstraints cc = new CellConstraints();

    this.add(createFilterPanel(), cc.xy(2, 2));

    JTabbedPane tabPane = new JTabbedPane();
    tabPane.add(ctx.getTextProvider().getString("listeners.tab.history"), this.createHistory());
    tabPane.add(ctx.getTextProvider().getString("listeners.tab.statistics"), this.createAvgPanel());
    this.add(tabPane, cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    this.entriesHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updateAvg();

      }
    });

    this.update();
  }

  private JPanel createHistory() {
    JPanel panel = new JPanel(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,80dlu,5dlu,pref:grow,2dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    this.listenersChart = new ListenersChart(this.ctx.getTextProvider());
    this.entriesHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      @SuppressWarnings("unchecked")
      public void propertyChange(PropertyChangeEvent evt) {
        updateChart((List<ListenersEntry>) evt.getNewValue(), null);
      }
    });
    panel.add(this.listenersChart, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    panel.add(createListenersTablePanel(), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));
    panel.add(createStatusBar(), cc.xy(2, 6, CellConstraints.FILL, CellConstraints.FILL));

    return panel;
  }

  @SuppressWarnings("rawtypes")
  private JPanel createAvgPanel() {
    JPanel panel = new JPanel(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,80dlu:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    {
      ListenersAvgTableModel model = new ListenersAvgTableModel(this.ctx.getTextProvider(), this.entriesAvgHolder);
      JXTable table = new JXTable(model);
      table.getColumn(de.stationadmin.gui.loganalyzer.listeners.ListenersAvgTableModel.Column.FRACTION.ordinal()).setCellRenderer(
          new IntTableCellRenderer(null, "%"));
      table.getColumn(de.stationadmin.gui.loganalyzer.listeners.ListenersAvgTableModel.Column.LISTENERS.ordinal()).setCellRenderer(
          new IntTableCellRenderer(null));
      
      final JPopupMenu popup = new JPopupMenu();
      popup.add(TableExportUtils.getCopyToClipboardAction(table, ctx.getTextProvider()));
      popup.add(TableExportUtils.getExportToExcelAction(table, ctx.getTextProvider(), ctx.getTextProvider().getString("listeners.tab.statistics")));

      table.addMouseListener(new PopupListener(table, popup));      

      panel.add(new JScrollPane(table), cc.xy(2, 2));
    }

    {
      PropertyChangeListener updateListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          updateAvg();
        }
      };
      this.granularity.addValueChangeListener(updateListener);
      this.weekdays.addValueChangeListener(updateListener);

      JPanel ctrlPanel = new JPanel(new FormLayout("pref,5dlu,pref", "pref,5dlu,pref"));
      Integer[] granularityValues = new Integer[] { 1, 2, 3, 4, 6, 8, 12 };
      SelectionInList<Integer> granularitySelection = new SelectionInList<Integer>(granularityValues, this.granularity);
      JComboBox gCmb = BasicComponentFactory.createComboBox(granularitySelection);
      JPanel gPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      gPanel.add(gCmb);
      gPanel.add(new JLabel(" " + ctx.getTextProvider().getString("listeners.avg.property.granularity.unit")));

      ctrlPanel.add(new JLabel(ctx.getTextProvider().getString("listeners.avg.property.granularity") + ":"), cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.CENTER));
      ctrlPanel.add(gPanel, cc.xy(3, 1));

      JPanel wdPanel = new JPanel(new GridLayout(1, 7));
      List<JCheckBox> wdCheckboxes = new ArrayList<JCheckBox>();
      WeekdayUpdater updater = new WeekdayUpdater(wdCheckboxes);
      for (Weekday day : Weekday.values()) {
        JCheckBox cb = new JCheckBox(ctx.getTextProvider().getString("weekday." + day.name().toLowerCase() + ".short"));
        cb.putClientProperty("weekday", day);
        cb.setSelected(true);
        cb.addActionListener(updater);
        wdPanel.add(cb);
        wdCheckboxes.add(cb);
      }

      ctrlPanel.add(new JLabel(ctx.getTextProvider().getString("listeners.avg.property.weekdays") + ":"), cc.xy(1, 3));
      ctrlPanel.add(wdPanel, cc.xy(3, 3));

      panel.add(ctrlPanel, cc.xy(2, 4));
    }

    return panel;

  }

  private JComponent createStatusBar() {
    final JXStatusBar statusBar = new JXStatusBar();
    statusBar.setOpaque(false);

    PresentationModel<ListenersStatistics> model = new PresentationModel<ListenersStatistics>(this.stats);
    JXStatusBar.Constraint minConst = new JXStatusBar.Constraint(new Insets(0, 2, 0, 2));
    PropertyPanel min = new PropertyPanel("Min:", model.getModel("min"));
    statusBar.add(min, minConst);

    JXStatusBar.Constraint maxConst = new JXStatusBar.Constraint(new Insets(0, 2, 0, 2));
    PropertyPanel max = new PropertyPanel("Max:", model.getModel("max"));
    statusBar.add(max, maxConst);

    JXStatusBar.Constraint avgConst = new JXStatusBar.Constraint(new Insets(0, 2, 0, 2));
    PropertyPanel avg = new PropertyPanel("Durchschnitt:", model.getModel("avg"));
    statusBar.add(avg, avgConst);

    JXStatusBar.Constraint tlhConst = new JXStatusBar.Constraint(new Insets(0, 2, 0, 2));
    PropertyPanel tlh = new PropertyPanel("Hördauer: ca ", model.getModel("tlh"), " Stunden");
    statusBar.add(tlh, tlhConst);

    this.entriesHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      @SuppressWarnings("unchecked")
      public void propertyChange(PropertyChangeEvent evt) {
        stats.update((List<ListenersEntry>) evt.getNewValue());
        statusBar.validate();
        statusBar.repaint();
      }
    });

    return statusBar;
  }

  private void updateChart(List<ListenersEntry> entries, Date centerTime) {
    if(entries.size() == 0) {
      return;
    }

    int startIdx = 0;
    if (centerTime != null) {
      long t = centerTime.getTime() - 1000 * 60 * 60 * 12;
      long lastTime = entries.get(entries.size() - 1).getTime().getTime();
      if (centerTime.getTime() + 1000 * 60 * 60 * 12 > lastTime) {
        t = lastTime - 1000 * 60 * 60 * 24;
      }
      while (t > entries.get(startIdx).getTime().getTime() && startIdx < entries.size()) {
        startIdx++;
      }
    }

    final int[] minutes = new int[60 * 24];
    int peak = 0;
    long offset = entries.get(startIdx).getTime().getTime();
    for (int i = startIdx; i < entries.size(); i++) {
      ListenersEntry entry = entries.get(i);
      int minute = i == startIdx ? 0 : (int) ((entry.getTime().getTime() - offset) / 60000);
      if (minute < minutes.length) {
        int nextMinute = i < entries.size() - 1 ? (int) ((entries.get(i + 1).getTime().getTime() - offset) / 60000) : Math.min(minutes.length,
            minute + 10);

        for (int m = minute; m < nextMinute && m < minutes.length; m++) {
          minutes[m] = entry.getListeners();
        }
      }
      peak = Math.max(peak, entry.getListeners());
    }
    this.listenersChart.update(offset, minutes, peak);
    if (centerTime != null) {
      this.listenersChart.highlight(centerTime.getTime());
    } else {
      this.listenersChart.clearHighlight();
    }
  }

  private JPanel createFilterPanel() {
    JPanel panel = new JPanel(new FormLayout("pref,2dlu,pref,2dlu,pref,2dlu,pref,2dlu,pref,2dlu,pref,5dlu:grow,pref", "pref"));
    CellConstraints cc = new CellConstraints();

    this.fromTime = new TimeEditor(this.ctx.getTextProvider(), new Date(System.currentTimeMillis() - LogAnalyzerService.DAY_IN_MS));
    this.toTime = new TimeEditor(this.ctx.getTextProvider(), new Date(System.currentTimeMillis()));

    panel.add(fromTime.getDateChooser(), cc.xy(1, 1));
    panel.add(fromTime.getTimePanel(), cc.xy(3, 1));
    panel.add(new JLabel("-"), cc.xy(5, 1));
    panel.add(toTime.getDateChooser(), cc.xy(7, 1));
    panel.add(toTime.getTimePanel(), cc.xy(9, 1));

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
    panel.add(toolbar, cc.xy(11, 1, CellConstraints.FILL, CellConstraints.FILL));

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

    panel.add(filterBtn, cc.xy(13, 1));

    return panel;
  }

  private JPanel createListenersTablePanel() {
    JPanel panel = new JPanel(new FormLayout("pref:grow", "80dlu:grow"));

    final ListenersTableModel model = new ListenersTableModel(this.ctx.getTextProvider(), entriesHolder);
    final JXTable table = new JXTable(model);
    table.getColumn(Column.DATE.ordinal()).setCellRenderer(
        new DateTableCellRenderer(new SimpleDateFormat(ctx.getTextProvider().getString("extDateFormat"))));
    table.getColumn(Column.TIME.ordinal()).setCellRenderer(
        new DateTableCellRenderer(new SimpleDateFormat(ctx.getTextProvider().getString("timeOnlyFormat"))));

    int timeWidth = ComponentFactory.getTableColumnWidthTime();
    int dateWidth = ComponentFactory.getTableColumnWidthDate();
    table.getColumnModel().getColumn(Column.DATE.ordinal()).setPreferredWidth(dateWidth);
    table.getColumnModel().getColumn(Column.DATE.ordinal()).setMaxWidth(dateWidth);
    table.getColumnModel().getColumn(Column.TIME.ordinal()).setPreferredWidth(timeWidth);
    table.getColumnModel().getColumn(Column.TIME.ordinal()).setMaxWidth(timeWidth);

    panel.add(new JScrollPane(table), new CellConstraints(1, 1, CellConstraints.FILL, CellConstraints.FILL));

    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      @Override
      @SuppressWarnings("unchecked")
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          int row = table.getSelectedRow();
          if (row > -1) {
            row = table.convertRowIndexToModel(row);

            List<ListenersEntry> entries = (List<ListenersEntry>) entriesHolder.getValue();
            ListenersEntry selected = entries.get(row);
            updateChart(entries, selected.getTime());
          } else {
            listenersChart.clearHighlight();
          }

        }

      }
    });
    
    final JPopupMenu popup = new JPopupMenu();
    popup.add(TableExportUtils.getCopyToClipboardAction(table, ctx.getTextProvider()));
    popup.add(TableExportUtils.getExportToExcelAction(table, ctx.getTextProvider(), ctx.getTextProvider().getString("listeners.tab.history"), 1 << Column.TIME.ordinal()));

    table.addMouseListener(new PopupListener(table, popup));    
    SwingTools.bindPopup(table, popup);

    return panel;
  }

  private void update() {
    try {
      entriesHolder.setValue(this.ctx.getAdminClient().getLogAnalyzerService().getListenersBetween(this.fromTime.getDate(), this.toTime.getDate()));
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @SuppressWarnings("unchecked")
  private void updateAvg() {
    List<ListenersEntry> rawEntries = (List<ListenersEntry>) this.entriesHolder.getValue();
    if (rawEntries != null) {
      this.entriesAvgHolder.setValue(this.ctx.getAdminClient().getLogAnalyzerService()
          .getAverageListenersInDay(rawEntries, (Integer) weekdays.getValue(), (Integer) granularity.getValue()));
    }

  }

  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(500, 400);
  }

  private class WeekdayUpdater implements ActionListener {
    private List<JCheckBox> checkboxes;

    public WeekdayUpdater(List<JCheckBox> checkboxes) {
      this.checkboxes = checkboxes;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int newValue = 0;
      for (JCheckBox cb : this.checkboxes) {
        if (cb.isSelected()) {
          Weekday day = (Weekday) cb.getClientProperty("weekday");
          newValue |= (1 << day.getCalDay());
        }
      }
      weekdays.setValue(newValue);

    }

  }

}
