/**
 * 
 */
package de.stationadmin.gui.loganalyzer.plays;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.loganalyzer.PlayFilter;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.schedule.Schedule.Entry;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.loganalyzer.util.SetTimeAction;
import de.stationadmin.gui.loganalyzer.util.TimeEditor;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ComponentFactory;

/**
 * @author korf
 * 
 */
public class PlayFilterPanel extends JPanel {
  private static final long serialVersionUID = -310378755826624532L;

  private TextProvider textProvider;
  private PlaylistRegistry playlistRegistry;
  private PlayFilter filter;

  TimeEditor fromTime;
  TimeEditor toTime;

  public PlayFilterPanel(TextProvider textProvider, PlayFilter filter, PlaylistRegistry playlistRegistry, ActionListener actionListener) {
    super();
    this.textProvider = textProvider;
    this.filter = filter;
    this.playlistRegistry = playlistRegistry;
    this.init(actionListener);
  }

  @SuppressWarnings("rawtypes")
  private void init(final ActionListener actionListener) {
    this.setLayout(new FormLayout("pref,3dlu,pref,5dlu:grow,pref", "pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    this.add(new JLabel(textProvider.getString("playsanalyzer.filter.time") + ":"), cc.xy(1, 1));
    this.add(this.createTimeSelector(), cc.xy(3, 1));

    final PresentationModel<PlayFilter> filterModel = new PresentationModel<PlayFilter>(this.filter);

    this.add(new JLabel(textProvider.getString("playsanalyzer.filter.artist") + ":"), cc.xy(1, 3));

    JPanel metadataPanel = new JPanel(new FormLayout("pref,8dlu,pref,5dlu,pref,8dlu,pref,5dlu,min(pref;100dlu),8dlu,pref,5dlu,min(pref;100dlu),8dlu,pref", "pref"));
    ComponentFactory componentFactory = new ComponentFactory(textProvider);
    JTextField artistTf = componentFactory.createTextField(filterModel.getModel("artist"));
    artistTf.setColumns(13);
    metadataPanel.add(artistTf, cc.xy(1, 1));

    metadataPanel.add(new JLabel(textProvider.getString("playsanalyzer.filter.title") + ":"), cc.xy(3, 1));
    JTextField titelTf = componentFactory.createTextField(filterModel.getModel("title"));
    titelTf.setColumns(13);
    metadataPanel.add(titelTf, cc.xy(5, 1));

    List<Playlist> playlists = new ArrayList<Playlist>();
    playlists.add(null);
    List<Playlist> online = this.playlistRegistry.getPlaylists(PlaylistType.ONLINE);
    Collections.sort(online, new PlaylistNameCompator());

    Set<Integer> scheduledPlaylists = new HashSet<Integer>();
    for (Entry entry : this.filter.getSchedule().getEntries()) {
      scheduledPlaylists.add(entry.getPlaylistId());
    }
    for (Playlist playlist : online) {
      if (scheduledPlaylists.contains(playlist.getId())) {
        playlists.add(playlist);
      }
    }

    SelectionInList<Playlist> playlistSelection = new SelectionInList<Playlist>(playlists, filterModel.getModel("playlist"));
    JComboBox playlistCmb = BasicComponentFactory.createComboBox(playlistSelection);
    metadataPanel.add(new JLabel(textProvider.getString("playsanalyzer.filter.playlist") + ":"), cc.xy(7, 1));
    metadataPanel.add(playlistCmb, cc.xy(9, 1));

    int colIdx = 11;
    if (filter.getTagManager() != null) {
      long t = System.currentTimeMillis();
      List<String> allTags = filter.getTagManager().getTags();
      SelectionInList<String> tagSelection = new SelectionInList<String>(allTags, filterModel.getModel("tag"));
      JComboBox tagCmb = BasicComponentFactory.createComboBox(tagSelection);

      metadataPanel.add(new JLabel(textProvider.getString("playsanalyzer.filter.tag") + ":"), cc.xy(colIdx, 1));
      colIdx += 2;
      metadataPanel.add(tagCmb, cc.xy(colIdx, 1));
      colIdx += 2;
    }

    JCheckBox musicOnlyCb = BasicComponentFactory.createCheckBox(filterModel.getModel("musicOnly"), textProvider.getString("playsanalyzer.filter.musiconly"));
    metadataPanel.add(musicOnlyCb, cc.xy(colIdx, 1));
    colIdx += 2;

    this.add(metadataPanel, cc.xy(3, 3));

    JButton filterBtn = new JButton(AppUtils.getIcon("filter.png"));
    filterBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        filter.setFromTime(fromTime.getDate());
        filter.setToTime(toTime.getDate());
        actionListener.actionPerformed(e);
      }
    });

    this.add(filterBtn, cc.xy(5, 3));

  }

  private JPanel createTimeSelector() {
    JPanel panel = new JPanel(new FormLayout("pref,2dlu,pref,5dlu,pref,5dlu,pref,2dlu,pref,2dlu,pref", "pref"));
    CellConstraints cc = new CellConstraints();

    fromTime = new TimeEditor(this.textProvider, this.filter.getFromTime());
    toTime = new TimeEditor(this.textProvider, this.filter.getToTime());

    panel.add(fromTime.getDateChooser(), cc.xy(1, 1));
    panel.add(fromTime.getTimePanel(), cc.xy(3, 1));

    panel.add(new JLabel("-"), cc.xy(5, 1));
    panel.add(toTime.getDateChooser(), cc.xy(7, 1));
    panel.add(toTime.getTimePanel(), cc.xy(9, 1));

    final JPopupMenu menu = new JPopupMenu();
    menu.add(new JMenuItem(new SetTimeAction(this.textProvider, this.fromTime, this.toTime, 0)));
    menu.add(new JMenuItem(new SetTimeAction(this.textProvider, this.fromTime, this.toTime, 1)));
    menu.add(new JMenuItem(new SetTimeAction(this.textProvider, this.fromTime, this.toTime, 2)));

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

    return panel;
  }

}
