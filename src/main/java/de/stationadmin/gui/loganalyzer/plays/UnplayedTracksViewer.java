/**
 * 
 */
package de.stationadmin.gui.loganalyzer.plays;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.loganalyzer.LogAnalyzerService;
import de.stationadmin.base.loganalyzer.Play;
import de.stationadmin.base.loganalyzer.PlayFilter;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.loganalyzer.plays.TrackTableModel.Column;
import de.stationadmin.gui.playlist.PopupListener;
import de.stationadmin.gui.track.CopyTracksAction;
import de.stationadmin.gui.track.DistributeTracksAction;
import de.stationadmin.gui.track.PlaySnippetAction;
import de.stationadmin.gui.track.TagMenu;
import de.stationadmin.gui.track.TrackViewAction;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.TableExportUtils;

/**
 * @author korf
 * 
 */
@SuppressWarnings("rawtypes")
public class UnplayedTracksViewer extends StationAdminFrame {
  private static final long serialVersionUID = 8894794909729373332L;
  private PlayFilter filter;

  private List<Play> loadedPlays;
  private int loadedPlaysDateHash;

  private ValueHolder playlistHolder = new ValueHolder();
  private ValueHolder tagHolder = new ValueHolder();
  private ValueHolder numTitlesHolder = new ValueHolder(0);

  private TrackTableModel tableModel;

  /**
   * @param ctx
   * @throws HeadlessException
   */
  public UnplayedTracksViewer(ClientContext ctx) throws HeadlessException {
    super(ctx, "plays.unplayed");
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
    this.setTitle(textProvider.getString("unplayedtitles.title"));
    this.filter = new PlayFilter();
    this.filter.setSchedule(this.ctx.getAdminClient().getSchedule());
    this.filter.setFromTime(toFullMinute(System.currentTimeMillis() - LogAnalyzerService.DAY_IN_MS));
    this.filter.setToTime(toFullMinute(System.currentTimeMillis()));

    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref:grow,2dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    ActionListener filterListener = new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {

        update();
      }
    };

    PlayFilterPanel filterPanel = new PlayFilterPanel(ctx.getTextProvider(), this.filter, ctx.getAdminClient().getPlaylistService().getPlaylistRegistry(), filterListener);
    this.getContentPane().add(filterPanel, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    {
      this.tableModel = new TrackTableModel(ctx.getTextProvider());
      final JXTable table = new JXTable(this.tableModel);
      int timeWidth = ComponentFactory.getTableColumnWidthTime();
      table.getColumnModel().getColumn(Column.LENGTH.ordinal()).setPreferredWidth(timeWidth);
      table.getColumnModel().getColumn(Column.LENGTH.ordinal()).setMaxWidth(timeWidth);
      this.getContentPane().add(new JScrollPane(table), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

      final ValueHolder titleHolder = new ValueHolder();

      table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          List<BasicTrack> titles = new ArrayList<BasicTrack>();
          int[] rows = table.getSelectedRows();
          for (int row : rows) {
            row = table.convertRowIndexToModel(row);
            BasicTrack t = tableModel.getTracks().get(row);
            titles.add(t);
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
          List<BasicTrack> titles = (List<BasicTrack>) evt.getNewValue();
          int[] ids = new int[titles.size()];
          for (int i = 0; i < ids.length; i++) {
            ids[i] = titles.get(i).getId();
          }
          tagMenu.setTitleIds(ids);
          untagMenu.setTitleIds(ids);
          copyAction.setTitles(titles);
          distributeAction.setTitles(titles);
          viewAction.setTitles(titles);
        }
      });

      final JPopupMenu popup = new JPopupMenu();
      popup.add(TableExportUtils.getCopyToClipboardAction(table, ctx.getTextProvider()));
      popup.add(TableExportUtils.getExportToExcelAction(table, ctx.getTextProvider(), ctx.getTextProvider().getString("unplayedtitles.title")));
      popup.addSeparator();
      popup.add(tagMenu);
      popup.add(untagMenu);
      popup.addSeparator();
      popup.add(copyAction);
      popup.add(distributeAction);
      popup.addSeparator();
      popup.add(viewAction);
      popup.add(new PlaySnippetAction(ctx, titleHolder));

      table.addMouseListener(new PopupListener(table, popup));
    }

    JXStatusBar statusBar = new JXStatusBar();
    statusBar.setOpaque(false);

    PropertyChangeListener updateListener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        update();
      }
    };

    // add playlist selector
    {
      JPanel playlistSelectorPanel = new JPanel(new FormLayout("pref,2dlu,pref", "max(pref;16dlu)"));
      playlistSelectorPanel.add(new JLabel(textProvider.getString("unplayedtitles.selector.playlist")), cc.xy(1, 1));
      List<Playlist> playlists = this.ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE);
      Collections.sort(playlists, new PlaylistNameCompator());
      SelectionInList<Playlist> playlistSelection = new SelectionInList<Playlist>(playlists, this.playlistHolder);
      JComboBox playlistCmb = BasicComponentFactory.createComboBox(playlistSelection);
      playlistSelectorPanel.add(playlistCmb, cc.xy(3, 1));

      statusBar.add(playlistSelectorPanel, new JXStatusBar.Constraint());

      this.playlistHolder.addValueChangeListener(updateListener);
    }

    // add tag selector
    {
      JPanel tagSelectorPanel = new JPanel(new FormLayout("pref,2dlu,pref", "max(pref;16dlu)"));
      tagSelectorPanel.add(new JLabel(textProvider.getString("unplayedtitles.selector.tag")), cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.CENTER));
      List<String> tags = this.ctx.getAdminClient().getTagManager().getTags();
      Collections.sort(tags);
      SelectionInList<String> tagSelection = new SelectionInList<String>(tags, this.tagHolder);
      JComboBox tagCmb = BasicComponentFactory.createComboBox(tagSelection);
      tagSelectorPanel.add(tagCmb, cc.xy(3, 1, CellConstraints.LEFT, CellConstraints.CENTER));

      statusBar.add(tagSelectorPanel, new JXStatusBar.Constraint());

      this.tagHolder.addValueChangeListener(updateListener);

    }

    // add status label
    {
      JPanel numPanel = new JPanel(new FormLayout("pref,2dlu,pref", "max(pref;16dlu)"));
      JLabel numLabel = BasicComponentFactory.createLabel(this.numTitlesHolder, NumberFormat.getIntegerInstance());
      numPanel.add(numLabel, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.CENTER));
      numPanel.add(new JLabel(textProvider.getString("titlelist.numTitles")), cc.xy(3, 1, CellConstraints.LEFT, CellConstraints.CENTER));

      statusBar.add(numPanel, new JXStatusBar.Constraint());
    }

    this.getContentPane().add(statusBar, cc.xy(2, 6, CellConstraints.LEFT, CellConstraints.CENTER));

    this.update();

  }

  private void update() {
    try {
      int filterDateHash = this.filter.getFromTime().hashCode() ^ this.filter.getToTime().hashCode();
      if (filterDateHash != this.loadedPlaysDateHash) {

        this.loadedPlays = this.ctx.getAdminClient().getLogAnalyzerService().getPlaysBetween(this.filter.getFromTime(), this.filter.getToTime());
        this.loadedPlaysDateHash = this.filter.getFromTime().hashCode() ^ this.filter.getToTime().hashCode();
      }

      HashSet<Integer> playedTitles = new HashSet<Integer>();
      List<Play> plays = filter.apply(this.loadedPlays);
      for (Play play : plays) {
        playedTitles.add(play.getTrack().getId());
      }

      HashSet<Integer> included = null;
      Playlist playlist = (Playlist) this.playlistHolder.getValue();
      if (playlist != null) {
        if (included == null)
          included = new HashSet<Integer>();
        for (Entry entry : playlist.getEntries()) {
          included.add(entry.getTrackId());
        }
      }

      String tag = (String) this.tagHolder.getValue();
      if (tag != null) {
        if (included == null)
          included = new HashSet<Integer>();
        int[] ids = ctx.getAdminClient().getTagManager().getTrackIds(tag);
        for (int id : ids) {
          included.add(id);
        }
      }

      List<RegisteredTrack> titles = new ArrayList<RegisteredTrack>();
      for (RegisteredTrack title : ctx.getAdminClient().getTrackService().getTrackRegistry().getAllTracks()) {
        if (included == null || included.contains(title.getId())) {
          if (!playedTitles.contains(title.getId())) {
            titles.add(title);
          }

        }

      }
      this.tableModel.setTracks(titles);
      this.numTitlesHolder.setValue(titles.size());

    } catch (Exception e) {
      JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.createErrorInfo(e, "playsanalyzer.load.error"));
    }

  }

}
