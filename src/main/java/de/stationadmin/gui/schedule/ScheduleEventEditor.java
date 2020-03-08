/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.apache.commons.lang3.SystemUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.toedter.calendar.JDateChooser;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.schedule.Schedule.Event;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.schedule.ScheduleEventTableModel.Column;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.DateTableCellRenderer;
import de.stationadmin.gui.util.IntTableCellRenderer;

/**
 * @author korf
 *
 */
public class ScheduleEventEditor extends JPanel {
  private static final long serialVersionUID = 128147028932022725L;
  private ClientContext ctx;
  private ScheduleEventTableModel tableModel;
  private JDateChooser dateChooser;
  private JComboBox<Integer> hourChooser;
  private JComboBox<Playlist> playlistChooser;
  private JComboBox<Integer> duarationChooser;

  /**
   * 
   */
  public ScheduleEventEditor(ClientContext ctx) {
    this.ctx = ctx;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref:grow,8dlu,pref,3dlu"));
    CellConstraints cc = new CellConstraints();

    // set up table
    {
      tableModel = new ScheduleEventTableModel(this.ctx.getAdminClient().getPlaylistService().getPlaylistRegistry(), ctx.getTextProvider());
      tableModel.setEvents(this.ctx.getAdminClient().getSchedule().getEvents());

      final JXTable table = new JXTable(tableModel);

      int timeWidth = ComponentFactory.getTableFontWidth(15);
      int dateWidth = ComponentFactory.getTableFontWidth(20);
      table.getColumnModel().getColumn(Column.STARTDATE.ordinal()).setPreferredWidth(dateWidth);
      table.getColumnModel().getColumn(Column.STARTDATE.ordinal()).setMaxWidth(dateWidth);
      table.getColumn(Column.STARTDATE.ordinal()).setCellRenderer(new DateTableCellRenderer(new SimpleDateFormat(ctx.getTextProvider().getString("extDateFormat"))));

      table.getColumnModel().getColumn(Column.STARTTIME.ordinal()).setPreferredWidth(timeWidth);
      table.getColumnModel().getColumn(Column.STARTTIME.ordinal()).setMaxWidth(timeWidth);
      table.getColumn(Column.STARTTIME.ordinal()).setCellRenderer(new DateTableCellRenderer(new SimpleDateFormat(ctx.getTextProvider().getString("timeOnlyFormat"))));

      table.getColumnModel().getColumn(Column.DURATION.ordinal()).setPreferredWidth(timeWidth);
      table.getColumnModel().getColumn(Column.DURATION.ordinal()).setMaxWidth(timeWidth);
      table.getColumn(Column.DURATION.ordinal()).setCellRenderer(new IntTableCellRenderer(0, " " + ctx.getString("scheduleeditor.event.duration.unit")));

      this.add(new JScrollPane(table), cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

      final JPopupMenu popup = new JPopupMenu();
      final DeleteAction deleteAction = new DeleteAction();
      popup.add(deleteAction);
      popup.add(new DeleteExpiredAction());

      table.addMouseListener(new MouseAdapter() {

        private void checkPopup(MouseEvent e) {
          if (e.isPopupTrigger()) {
            popup.show(table, e.getX(), e.getY());
          }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
          int row = table.rowAtPoint(e.getPoint());
          if (row > -1) {
            row = table.convertRowIndexToModel(row);
            Event evt = tableModel.getEvents().get(row);
            deleteAction.setEvent(evt);
          } else {
            deleteAction.setEvent(null);
          }
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

    this.add(createEventEditPanel(), cc.xy(2, 4));

  }

  private JPanel createEventEditPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref,5dlu,pref,3dlu", "3dlu,pref,5dlu,pref,5dlu,pref,3dlu"));

    panel.setBorder(BorderFactory.createTitledBorder(ctx.getString("scheduleeditor.event.create.title")));

    CellConstraints cc = new CellConstraints();

    int row = 2;

    panel.add(new JLabel(ctx.getString("scheduleeditor.event.create.date")), cc.xy(2, row));
    
    String hourWidth = SystemUtils.IS_OS_LINUX ? "35dlu" : "pref";

    JPanel timeDurationPanel = new JPanel(new FormLayout("pref,3dlu,pref,3dlu," + hourWidth + ",3dlu,pref,3dlu,pref,3dlu,pref,3dlu,pref", "pref"));

    this.dateChooser = new JDateChooser();
    this.dateChooser.setLocale(ctx.getTextProvider().getLocale());
    this.dateChooser.setDateFormatString(ctx.getTextProvider().getString("dateFormat"));
    this.dateChooser.setDate(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24));
    timeDurationPanel.add(this.dateChooser, cc.xy(1, 1));

    timeDurationPanel.add(new JLabel(ctx.getString("scheduleeditor.event.create.time")), cc.xy(3, 1));
    this.hourChooser = new JComboBox<Integer>();
    for (int i = 0; i < 24; i++) {
      hourChooser.addItem(i);
    }
    hourChooser.setSelectedIndex(12);
    timeDurationPanel.add(hourChooser, cc.xy(5, 1));
    timeDurationPanel.add(new JLabel(ctx.getString("scheduleeditor.event.create.time.after")), cc.xy(7, 1));

    timeDurationPanel.add(new JLabel(ctx.getString("scheduleeditor.event.create.duration")), cc.xy(9, 1));
    this.duarationChooser = new JComboBox<Integer>();
    for (int i = 1; i <= 24; i++) {
      duarationChooser.addItem(i);
    }
    timeDurationPanel.add(duarationChooser, cc.xy(11, 1));
    timeDurationPanel.add(new JLabel(ctx.getString("scheduleeditor.event.create.duration.after")), cc.xy(13, 1));

    panel.add(timeDurationPanel, cc.xy(4, row));
    row += 2;

    // Playlist selection
    panel.add(new JLabel(ctx.getString("scheduleeditor.event.create.playlist")), cc.xy(2, row));
    List<Playlist> playlists = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE);
    java.util.Collections.sort(playlists, new PlaylistNameCompator());
    this.playlistChooser = new JComboBox<Playlist>(playlists.toArray(new Playlist[playlists.size()]));
    panel.add(playlistChooser, cc.xy(4, row));
    row += 2;

    // Ok-Button
    panel.add(new JButton(new AddAction()), cc.xywh(2, row, 3, 1, CellConstraints.RIGHT, CellConstraints.CENTER));

    return panel;
  }

  void addEvent() {

    Calendar cal = Calendar.getInstance();
    if (this.dateChooser.getDate() == null) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(null, "scheduleeditor.event.errmsg.create.nodate"));
      return;
    }
    cal.setTime(this.dateChooser.getDate());
    cal.set(Calendar.HOUR_OF_DAY, (Integer) hourChooser.getSelectedItem());
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);

    int playlistId = ((Playlist) this.playlistChooser.getSelectedItem()).getId();
    int duration = (Integer) this.duarationChooser.getSelectedItem();

    try {
      ctx.getAdminClient().getSchedule().scheduleEvent(new Event(playlistId, cal.getTime(), duration));
      tableModel.setEvents(ctx.getAdminClient().getSchedule().getEvents());
      JOptionPane.showMessageDialog(this, "Die Sendung wurde eingetragen");
    } catch (Exception e) {
      JXErrorPane.showDialog(this, ctx.createErrorInfo(e, "scheduleeditor.event.errmsg.create.general"));
    }

  }

  private class AddAction extends AbstractAction {
    private static final long serialVersionUID = -8668282097217970842L;

    AddAction() {
      putValue(Action.NAME, ctx.getString("scheduleeditor.event.action.create"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      addEvent();
    }

  }

  private class DeleteAction extends AbstractAction {
    private static final long serialVersionUID = 6990596239438311683L;
    private Event event;

    DeleteAction() {
      putValue(Action.NAME, ctx.getString("scheduleeditor.event.action.delete"));
      setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      try {
        ctx.getAdminClient().getSchedule().deleteEvent(event);
        setEnabled(false);
        tableModel.setEvents(ctx.getAdminClient().getSchedule().getEvents());
      } catch (Exception e) {
        JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "scheduleeditor.event.errmsg.create.delete"));
      }
    }

    public void setEvent(Event event) {
      this.event = event;
      setEnabled(this.event != null);
    }

  }

  private class DeleteExpiredAction extends AbstractAction {
    private static final long serialVersionUID = 945149630823761844L;

    DeleteExpiredAction() {
      putValue(Action.NAME, ctx.getString("scheduleeditor.event.action.deleteExpired"));
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      try {
        int deleteCnt = 0;
        List<Event> events = new ArrayList<Event>(ctx.getAdminClient().getSchedule().getEvents());
        for (Event event : events) {
          if (System.currentTimeMillis() > event.getEndTime().getTime()) {
            ctx.getAdminClient().getSchedule().deleteEvent(event);
            tableModel.setEvents(ctx.getAdminClient().getSchedule().getEvents());
            deleteCnt++;
          }
        }
        if(deleteCnt == 0) {
          JOptionPane.showMessageDialog(ScheduleEventEditor.this, ctx.getString("scheduleeditor.event.errmsg.create.deleteExpire.nomatch"));
        }
        else {
          JOptionPane.showMessageDialog(ScheduleEventEditor.this, ctx.getString("scheduleeditor.event.info.delete", Integer.toString(deleteCnt)));
        }
      } catch (Exception e) {
        JXErrorPane.showDialog(ScheduleEventEditor.this, ctx.createErrorInfo(e, "scheduleeditor.event.errmsg.create.delete"));
      }
      tableModel.setEvents(ctx.getAdminClient().getSchedule().getEvents());

    }

  }

}
