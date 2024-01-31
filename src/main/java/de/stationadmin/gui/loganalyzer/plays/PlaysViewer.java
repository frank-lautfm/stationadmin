/**
 * 
 */
package de.stationadmin.gui.loganalyzer.plays;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.loganalyzer.Play;
import de.stationadmin.base.loganalyzer.PlayStatistics;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.loganalyzer.plays.PlaysTableModel.Column;
import de.stationadmin.gui.loganalyzer.util.PropertyPanel;
import de.stationadmin.gui.playlist.PopupListener;
import de.stationadmin.gui.track.CopyTracksAction;
import de.stationadmin.gui.track.DistributeTracksAction;
import de.stationadmin.gui.track.PlaySnippetAction;
import de.stationadmin.gui.track.TagMenu;
import de.stationadmin.gui.track.TrackViewAction;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.DateTableCellRenderer;
import de.stationadmin.gui.util.LengthTableCellRenderer;
import de.stationadmin.gui.util.SwingTools;
import de.stationadmin.gui.util.TableExportUtils;

/**
 * @author korf
 * 
 */
public class PlaysViewer extends JPanel {
  private static final long serialVersionUID = 2793953939211019933L;
  private ClientContext ctx;
  private TextProvider textProvider;
  private PlaysTableModel model;
  private PlayStatistics statistics;
  private JXTable table;

  public PlaysViewer(ClientContext ctx, ValueModel playsHolder, PlayStatistics statistics) {
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.model = new PlaysTableModel(ctx, playsHolder);
    this.statistics = statistics;
    this.init();
  }

  private void init() {
    this.setLayout(new BorderLayout());


    // table
    {
      SimpleDateFormat extDateFormat = new SimpleDateFormat(textProvider.getString("extDateFormat"));
      SimpleDateFormat timeFormat = new SimpleDateFormat(textProvider.getString("timeOnlyFormat"));
      
      table = new JXTable(this.model);
      table.getColumn(Column.START_DATE.ordinal()).setCellRenderer(
          new DateTableCellRenderer(extDateFormat));
      table.getColumn(Column.START_TIME.ordinal()).setCellRenderer(
          new DateTableCellRenderer(timeFormat));
      table.getColumn(Column.LENGTH.ordinal()).setCellRenderer(new LengthTableCellRenderer(false));
      
      int timeWidth = ComponentFactory.getTableColumnWidthTime();
      int dateWidth = ComponentFactory.getTableColumnWidthDate();
      int listenersWidth = ComponentFactory.getTableFontWidth(6);
      table.getColumnModel().getColumn(Column.LENGTH.ordinal()).setPreferredWidth(timeWidth);
      table.getColumnModel().getColumn(Column.LENGTH.ordinal()).setMaxWidth(timeWidth);
      table.getColumnModel().getColumn(Column.START_DATE.ordinal()).setPreferredWidth(dateWidth);
      table.getColumnModel().getColumn(Column.START_DATE.ordinal()).setMaxWidth(dateWidth);
      table.getColumnModel().getColumn(Column.START_TIME.ordinal()).setPreferredWidth(timeWidth);
      table.getColumnModel().getColumn(Column.START_TIME.ordinal()).setMaxWidth(timeWidth);
      table.getColumnModel().getColumn(Column.LISTENERS.ordinal()).setPreferredWidth(listenersWidth);
      table.getColumnModel().getColumn(Column.LISTENERS.ordinal()).setMaxWidth(listenersWidth);
      table.getColumnExt(table.convertColumnIndexToView(Column.SHOW.ordinal())).setVisible(false);
      table.getColumnExt(table.convertColumnIndexToView(Column.LISTENERS.ordinal())).setVisible(false);
      
      table.setColumnControlVisible(true);

      this.add(new JScrollPane(table));
      
      final ValueHolder titleHolder = new ValueHolder();
      
      table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
          List<BasicTrack> titles = new ArrayList<BasicTrack>();
          int[] rows = table.getSelectedRows();
          for(int row : rows) {
            row = table.convertRowIndexToModel(row);
            Play play = model.getPlays().get(row);
            titles.add(play.getTrack());
          }
          titleHolder.setValue(titles);
        }
      });
      
      final TagMenu tagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), true);
      final TagMenu untagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), false);
      final CopyTracksAction copyAction = new CopyTracksAction(this.ctx);
      final DistributeTracksAction distributeAction = new DistributeTracksAction(this.ctx);
      final TrackViewAction viewAction = new TrackViewAction(ctx);
      
      titleHolder.addValueChangeListener(new PropertyChangeListener() {
        
        @Override
        @SuppressWarnings("unchecked")
        public void propertyChange(PropertyChangeEvent evt) {
          List<BasicTrack> titles = (List<BasicTrack>)evt.getNewValue();
          int[] ids = new int[titles.size()];
          for(int i = 0; i < ids.length; i++) {
            ids[i] = titles.get(i).getId();
          }
          tagMenu.setTitleIds(ids);
          untagMenu.setTitleIds(ids);
          copyAction.setTitles(titles);
          distributeAction.setTitles(titles);
          viewAction.setTitles(titles);
        }
      });
      
      table.setTransferHandler(TableExportUtils.getTransferHandler(table));
      
      final JPopupMenu popup = new JPopupMenu();
      popup.add(TableExportUtils.getCopyToClipboardAction(table, ctx.getTextProvider()));
      popup.add(TableExportUtils.getExportToExcelAction(table, ctx.getTextProvider(), ctx.getTextProvider().getString("playsanalyzer.tab.plays"), 1 << Column.START_TIME.ordinal()));
      popup.addSeparator();
      popup.add(tagMenu);
      popup.add(untagMenu);
      popup.addSeparator();
      popup.add(copyAction);
      popup.add(distributeAction);
      popup.addSeparator();
      popup.add(viewAction);
      popup.add(new PlaySnippetAction(ctx, titleHolder));

      SwingTools.bindPopup(table, popup);

      table.addMouseListener(new PopupListener(table, popup));
    }
    
    

    // statistics
    {
      PresentationModel<PlayStatistics> statsModel = new PresentationModel<PlayStatistics>(this.statistics);

      JXStatusBar statusBar = new JXStatusBar();
      statusBar.setOpaque(false);

      JXStatusBar.Constraint playsConst = new JXStatusBar.Constraint(new Insets(2, 2, 2, 2));
      PropertyPanel numPlays = new PropertyPanel(this.textProvider.getString("play.stats.plays") + ":", statsModel.getModel("numPlays"));
      statusBar.add(numPlays, playsConst);

      JXStatusBar.Constraint titlesConst = new JXStatusBar.Constraint(new Insets(2, 2, 2, 2));
      PropertyPanel numTracks = new PropertyPanel(this.textProvider.getString("play.stats.titles") + ":", statsModel.getModel("numTracks"));
      statusBar.add(numTracks, titlesConst);

      JXStatusBar.Constraint artistsConst = new JXStatusBar.Constraint(new Insets(2, 2, 2, 2));
      PropertyPanel numArtists = new PropertyPanel(this.textProvider.getString("play.stats.artists") + ":", statsModel.getModel("numArtists"));
      statusBar.add(numArtists, artistsConst);
      
      JXStatusBar.Constraint scoreConst = new JXStatusBar.Constraint(new Insets(2, 2, 2, 2));
      PropertyPanel score = new PropertyPanel(this.textProvider.getString("play.stats.score") +  ":", statsModel.getModel("score"), "%");
      statusBar.add(score, scoreConst);

      this.statistics.addPropertyChangeListener("score", new PropertyChangeListener() {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          validate();
          repaint();
        }
      });

      this.add(statusBar, BorderLayout.SOUTH);
    }

  }


}
