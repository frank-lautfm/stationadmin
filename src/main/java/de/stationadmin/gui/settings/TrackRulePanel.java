package de.stationadmin.gui.settings;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.Settings;
import de.stationadmin.base.playlist.shuffle.TrackRule;
import de.stationadmin.base.playlist.shuffle.TrackRule.FilterType;
import de.stationadmin.base.playlist.shuffle.TrackRule.TrackPosition;
import de.stationadmin.base.playlist.shuffle.TrackRuleEngine.JingleCollisionStratagy;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup.MultiMatchSelection;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.EnumListCellRenderer;
import de.stationadmin.gui.util.EnumTableCellRenderer;

public class TrackRulePanel extends JPanel {
  private static final long serialVersionUID = 7997929549512395824L;
  private ClientContext ctx;
  private PresentationModel<Settings> model;

  public TrackRulePanel(ClientContext ctx, PresentationModel<Settings> model) {
    this.ctx = ctx;
    this.model = model;
    this.init();
  }

  @SuppressWarnings("unchecked")
  private void init() {

    this.setLayout(new FormLayout("100dlu:grow", "pref,5dlu,30dlu:grow,8dlu,pref,5dlu,50dlu:grow,8dlu,pref,3dlu,pref,8dlu,pref,3dlu,pref"));
    CellConstraints cc = new CellConstraints();

    this.add(new JLabel("Gruppen"), cc.xy(1, 1));

    List<TrackRuleGroup> groups = model.getBean().getTrackRuleGroups();
    if (groups == null) {
      groups = new ArrayList<TrackRuleGroup>();
      model.getBean().setTrackRuleGroups(groups);
    }
    if (groups.size() == 0) {
      groups.add(new TrackRuleGroup("Standard", 0));
    }

    TrackRuleGroupTableModel groupsModel = new TrackRuleGroupTableModel(ctx.getTextProvider(), groups);
    JXTable groupsTable = new JXTable(groupsModel);
    this.add(new JScrollPane(groupsTable), cc.xy(1, 3));
    {
      TableColumn colDist = groupsTable.getColumnModel().getColumn(2);
      colDist.setPreferredWidth(80);
      colDist.setMaxWidth(80);
    }
    {
      TableColumn colSelection = groupsTable.getColumnModel().getColumn(1);
      JComboBox<MultiMatchSelection> selectionCmb = new JComboBox<MultiMatchSelection>(
          new MultiMatchSelection[] { MultiMatchSelection.ALL, MultiMatchSelection.FIRST, MultiMatchSelection.RANDOM });
      selectionCmb.setRenderer(new EnumListCellRenderer(ctx.getTextProvider(), "settings.playlistgen.table.rule.selection"));
      colSelection.setCellEditor(new DefaultCellEditor(selectionCmb));
      colSelection.setCellRenderer(new EnumTableCellRenderer(ctx.getTextProvider(), "settings.playlistgen.table.rule.selection"));
      colSelection.setPreferredWidth(120);
      colSelection.setMaxWidth(120);
    }

    groupsModel.addNameChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        String old = (String) evt.getOldValue();
        String newName = (String) evt.getNewValue();
        if (StringUtils.isNotEmpty(old)) {
          for (TrackRule rule : model.getBean().getTrackRules()) {
            if (rule.getGroupName() != null && rule.getGroupName().equals(old)) {
              rule.setGroupName(StringUtils.trimToNull(newName));
            }
          }
        }

      }
    });

    this.add(new JLabel("Regeln"), cc.xy(1, 5));
    List<TrackRule> rules = model.getBean().getTrackRules();
    if (rules == null) {
      rules = new ArrayList<TrackRule>();
      model.getBean().setTrackRules(rules);
    }

    TrackRuleTableModel rulesModel = new TrackRuleTableModel(ctx.getTextProvider(), ctx.getAdminClient().getTrackService().getTrackRegistry(), rules, groups);
    final JXTable rulesTable = new JXTable(rulesModel);
    this.add(new JScrollPane(rulesTable), cc.xy(1, 7));

    {
      TableColumn colGroup = rulesTable.getColumnModel().getColumn(0);
      SelectionInList<TrackRuleGroup> groupSelection = new SelectionInList<TrackRuleGroup>(groups);
      JComboBox<TrackRuleGroup> groupCmb = BasicComponentFactory.createComboBox(groupSelection);
      colGroup.setCellEditor(new DefaultCellEditor(groupCmb));
    }

    {
      ArrayList<BasicTrack> jingles = new ArrayList<BasicTrack>();
      for (BasicTrack track : ctx.getAdminClient().getTrackService().getTrackRegistry().getAllTracks()) {
        if (track.getType() != BasicTrack.TYPE_MUSIC) {
          jingles.add(track);
        }
      }
      Collections.sort(jingles, new Comparator<BasicTrack>() {

        @Override
        public int compare(BasicTrack o1, BasicTrack o2) {
          int result = o1.getArtist().compareTo(o2.getArtist());
          if (result == 0) {
            result = o1.getTitle().compareTo(o2.getTitle());
          }
          return result;
        }
      });
      jingles.add(0, null);

      TableColumn colTrack = rulesTable.getColumnModel().getColumn(1);
      JComboBox<BasicTrack> positionCmb = new JComboBox<BasicTrack>(jingles.toArray(new BasicTrack[jingles.size()]));
      colTrack.setCellEditor(new DefaultCellEditor(positionCmb));

    }

    {
      TableColumn colPosition = rulesTable.getColumnModel().getColumn(2);
      JComboBox<TrackPosition> positionCmb = new JComboBox<TrackRule.TrackPosition>(new TrackPosition[] { TrackPosition.BEFORE, TrackPosition.AFTER });
      positionCmb.setRenderer(new EnumListCellRenderer(ctx.getTextProvider(), "settings.playlistgen.table.rule.position"));
      colPosition.setCellEditor(new DefaultCellEditor(positionCmb));
      colPosition.setCellRenderer(new EnumTableCellRenderer(ctx.getTextProvider(), "settings.playlistgen.table.rule.position"));
      colPosition.setPreferredWidth(60);
      colPosition.setMaxWidth(60);
    }

    {
      TableColumn colType = rulesTable.getColumnModel().getColumn(3);
      JComboBox<FilterType> filterCmb = new JComboBox<FilterType>(new FilterType[] { FilterType.ARTIST, FilterType.ARTIST_TITLE, FilterType.ALBUM, FilterType.TAG });
      filterCmb.setRenderer(new EnumListCellRenderer(ctx.getTextProvider(), "settings.playlistgen.table.rule.filter"));
      colType.setCellEditor(new DefaultCellEditor(filterCmb));
      colType.setCellRenderer(new EnumTableCellRenderer(ctx.getTextProvider(), "settings.playlistgen.table.rule.filter"));
      colType.setPreferredWidth(90);
      colType.setMaxWidth(90);
    }

    {
      TableColumn colDist = rulesTable.getColumnModel().getColumn(5);
      colDist.setPreferredWidth(50);
      colDist.setMaxWidth(50);
    }

    SelectionInList<MultiMatchSelection> groupCollisionStrategySelection = new SelectionInList<TrackRuleGroup.MultiMatchSelection>(
        new MultiMatchSelection[] { MultiMatchSelection.ALL, MultiMatchSelection.FIRST, MultiMatchSelection.RANDOM },
        model.getBufferedComponentModel("trackRuleGroupCollisionStrategy"));
    JComboBox<MultiMatchSelection> groupCollisionStrategyCmb = BasicComponentFactory.createComboBox(groupCollisionStrategySelection,
        (new EnumListCellRenderer(ctx.getTextProvider(), "settings.playlistgen.table.rulegroup.selection")));
    this.add(new JLabel(ctx.getString("settings.playlistgen.table.rule.group.collission")), cc.xy(1, 9));
    this.add(groupCollisionStrategyCmb, cc.xy(1, 11, CellConstraints.LEFT, CellConstraints.CENTER));

    SelectionInList<JingleCollisionStratagy> jcsSelection = new SelectionInList<JingleCollisionStratagy>(JingleCollisionStratagy.values(),
        model.getBufferedComponentModel("trackRuleJingleCollsisionStrategy"));
    JComboBox<JingleCollisionStratagy> jcsCmb = BasicComponentFactory.createComboBox(jcsSelection,
        new EnumListCellRenderer(this.ctx.getTextProvider(), "settings.playlistgen.table.rule.collission"));
    this.add(new JLabel(ctx.getString("settings.playlistgen.table.rule.collission")), cc.xy(1, 13));
    this.add(jcsCmb, cc.xy(1, 15, CellConstraints.LEFT, CellConstraints.CENTER));

    final JPopupMenu popup = new JPopupMenu();
    final MoveUpAction moveUpAction = new MoveUpAction(rulesModel);
    final MoveDownAction moveDownAction = new MoveDownAction(rulesModel);
    popup.add(moveUpAction);
    popup.add(moveDownAction);

    rulesTable.addMouseListener(new MouseAdapter() {

      private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
          int row = rulesTable.rowAtPoint(new Point(e.getX(), e.getY()));
          moveDownAction.setRow(row);
          moveUpAction.setRow(row);
          popup.show(rulesTable, e.getX(), e.getY());
        }
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        this.checkPopup(e);
      }

      /**
       * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
       */
      @Override
      public void mousePressed(MouseEvent e) {
        this.checkPopup(e);
      }

      /**
       * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
       */
      @Override
      public void mouseReleased(MouseEvent e) {
        this.checkPopup(e);
      }

    });

  }

  private class MoveUpAction extends AbstractAction {
    private static final long serialVersionUID = -6701577713786087406L;
    private TrackRuleTableModel model;
    private int row;

    public MoveUpAction(TrackRuleTableModel model) {
      this.model = model;
      this.putValue(Action.NAME, "Weiter nach oben");
      this.setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (row > -1) {
        model.moveUp(row);
      }
    }

    public void setRow(int row) {
      this.row = row;
      this.setEnabled(row > 0 && row < model.getRowCount() - 1);
    }

  }

  private class MoveDownAction extends AbstractAction {
    private static final long serialVersionUID = -6701577713786087406L;
    private TrackRuleTableModel model;
    private int row;

    public MoveDownAction(TrackRuleTableModel model) {
      this.model = model;
      this.putValue(Action.NAME, "Weiter nach unten");
      this.setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (row > -1) {
        model.moveDown(row);
      }
    }

    public void setRow(int row) {
      this.row = row;
      this.setEnabled(row < model.getRowCount() - 2);
    }

  }
}
