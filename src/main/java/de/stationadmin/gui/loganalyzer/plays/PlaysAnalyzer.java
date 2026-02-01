/**
 * 
 */
package de.stationadmin.gui.loganalyzer.plays;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.loganalyzer.LogAnalyzerService;
import de.stationadmin.base.loganalyzer.Play;
import de.stationadmin.base.loganalyzer.PlayFilter;
import de.stationadmin.base.loganalyzer.PlayStatistics;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.PopupListener;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.SwingTools;
import de.stationadmin.gui.util.TableExportUtils;

/**
 * @author korf
 * 
 */
public class PlaysAnalyzer extends StationAdminFrame {
  private static final long serialVersionUID = 4638908847461912066L;
  private PlayFilter filter;
  private PlayStatistics statistics;
  private ValueModel playsHolder = new ValueHolder(new ArrayList<Play>());

  private List<Play> loadedPlays;
  private int loadedPlaysDateHash;

  /**
   * @param ctx
   * @throws HeadlessException
   */
  public PlaysAnalyzer(ClientContext ctx) throws HeadlessException {
    super(ctx, "plays.analyzer");
    this.init();
  }

  private Date toFullMinute(long time) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(time);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
    
  }

  private void init() {
    TextProvider textProvider = this.ctx.getTextProvider();
    this.setTitle(textProvider.getString("playsanalyzer.title"));
    this.filter = new PlayFilter(this.ctx.getAdminClient().getTagManager());
    this.filter.setSchedule(this.ctx.getAdminClient().getSchedule());
    this.filter.setFromTime(toFullMinute(System.currentTimeMillis() - LogAnalyzerService.DAY_IN_MS));
    this.filter.setToTime(toFullMinute(System.currentTimeMillis()));

    this.statistics = new PlayStatistics();

    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref:grow,5dlu"));
    CellConstraints cc = new CellConstraints();

    ActionListener filterListener = new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {

        update();
      }
    };

    PlayFilterPanel filterPanel = new PlayFilterPanel(ctx.getTextProvider(), this.filter, ctx.getAdminClient().getPlaylistService()
        .getPlaylistRegistry(), filterListener);
    this.getContentPane().add(filterPanel, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    JTabbedPane tabPane = new JTabbedPane();
    tabPane.addTab(textProvider.getString("playsanalyzer.tab.plays"), new PlaysViewer(ctx, playsHolder, statistics));
    tabPane.addTab(textProvider.getString("playsanalyzer.tab.artists"), this.createArtistPanel());
    tabPane.addTab(textProvider.getString("playsanalyzer.tab.titles"), this.createTitlePanel());

    this.getContentPane().add(tabPane, cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    this.update();

  }
  
  private JPanel createArtistPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    JXTable table = new JXTable(new FrequentArtistTableModel(this.ctx.getTextProvider(), this.statistics));
    table.getColumn(0).setPreferredWidth(40);
    table.getColumn(0).setMaxWidth(40);
    
    final JPopupMenu popup = new JPopupMenu();
    popup.add(TableExportUtils.getCopyToClipboardAction(table, ctx.getTextProvider()));
    popup.add(TableExportUtils.getExportToExcelAction(table, ctx.getTextProvider(), ctx.getTextProvider().getString("playsanalyzer.tab.plays")));
    table.addMouseListener(new PopupListener(table, popup));    
    
    
    SwingTools.bindPopup(table, popup);

    panel.add(new JScrollPane(table), BorderLayout.CENTER);
    return panel;
  }

  private JPanel createTitlePanel() {
    JPanel panel = new JPanel(new BorderLayout());
    JXTable table = new JXTable(new FrequentTracksTableModel(this.ctx, this.statistics));
    table.getColumn(0).setPreferredWidth(40);
    table.getColumn(0).setMaxWidth(40);
    table.getColumnExt(3).setVisible(false);
    table.setColumnControlVisible(true);
    
    final JPopupMenu popup = new JPopupMenu();
    popup.add(TableExportUtils.getCopyToClipboardAction(table, ctx.getTextProvider()));
    popup.add(TableExportUtils.getExportToExcelAction(table, ctx.getTextProvider(), ctx.getTextProvider().getString("playsanalyzer.tab.plays")));
    table.addMouseListener(new PopupListener(table, popup));    
    SwingTools.bindPopup(table, popup);
    
    panel.add(new JScrollPane(table), BorderLayout.CENTER);
    return panel;
  }

  private void update() {

    try {
      int filterDateHash = this.filter.getFromTime().hashCode() ^ this.filter.getToTime().hashCode();
      if (filterDateHash != this.loadedPlaysDateHash) {

        this.loadedPlays = this.ctx.getAdminClient().getLogAnalyzerService().getPlaysBetween(this.filter.getFromTime(), this.filter.getToTime());
        this.loadedPlaysDateHash = this.filter.getFromTime().hashCode() ^ this.filter.getToTime().hashCode();
      }

      List<Play> plays = filter.apply(this.loadedPlays);
      this.playsHolder.setValue(plays);
      this.statistics.update(plays);

    } catch (Exception e) {
      JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.createErrorInfo(e, "playsanalyzer.load.error"));
    }

  }

}
