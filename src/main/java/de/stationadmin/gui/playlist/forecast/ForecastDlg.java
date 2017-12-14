/**
 * 
 */
package de.stationadmin.gui.playlist.forecast;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.schedule.PlaylistForecast;
import de.stationadmin.base.schedule.PlaylistForecast.ScheduledTrack;
import de.stationadmin.base.schedule.Schedule.Weekday;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.forecast.ForecastTableModel.Column;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.SwingTools;

/**
 * 
 * @author Frank Korf
 * 
 */
public class ForecastDlg extends JFrame {
  private static final long serialVersionUID = 6065961410236107681L;
  private ClientContext ctx;
  private PlaylistForecast forecast;
  private ForecastSettings settings = new ForecastSettings();
  private ForecastTableModel model;

  public ForecastDlg(ClientContext ctx) throws HeadlessException {
    super();
    this.ctx = ctx;
    this.forecast = new PlaylistForecast(ctx.getAdminClient().getPlaylistRegistry(), ctx.getAdminClient().getSchedule());
    this.model = new ForecastTableModel(ctx);
    this.init();
    this.updateForecast();
    this.settings.addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updateForecast();
      }

    });
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,100dlu:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    this.getContentPane().add(this.createSettingsPanel(), cc.xy(2, 2));
    this.getContentPane().add(this.createListPanel(), cc.xy(2, 4));
    JLabel label = new JLabel("Für geshuffelte Playlists kann keine korrekte Vorschau erstellt werden. Diese Playlists sind grau dargestellt.");
    this.getContentPane().add(label, cc.xy(2, 6));

    this.setTitle(ctx.getString("playlistforecast.title"));
    this.setSize(new Dimension(600, 500));
    SwingTools.centerOnScreen(this);
  }

  private JPanel createSettingsPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
    PresentationModel<ForecastSettings> pm = new PresentationModel<ForecastSettings>(this.settings);

    SelectionInList<Weekday> weekdaySelection = new SelectionInList<Weekday>(Weekday.values(), pm.getModel("weekday"));
    JComboBox weekdayCmb = BasicComponentFactory.createComboBox(weekdaySelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = -2247555179698781296L;

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText(ctx.getString("weekday." + ((Weekday) value).name().toLowerCase()));
        return this;
      }

    });
    panel.add(weekdayCmb);

    ArrayList<Integer> startHours = new ArrayList<Integer>();
    ArrayList<Integer> durationHours = new ArrayList<Integer>();
    for (int i = 0; i < 24; i++) {
      startHours.add(i);
      durationHours.add(i + 1);
    }

    JComboBox startHourTf = BasicComponentFactory.createComboBox(new SelectionInList<Integer>(startHours, pm.getModel("startHour")));
    JComboBox hoursTf = BasicComponentFactory.createComboBox(new SelectionInList<Integer>(durationHours, pm.getModel("hours")));

    panel.add(new JLabel(" " + ctx.getString("playlistforecast.label.from")));
    panel.add(startHourTf);
    panel.add(new JLabel(":00 " + ctx.getString("playlistforecast.label.for")));
    panel.add(hoursTf);
    panel.add(new JLabel(" " + ctx.getString("playlistforecast.label.hours")));

    return panel;
  }

  public void updateForecast() {
    List<ScheduledTrack> titles = this.forecast.generateForecast(settings.getStartTime(), settings.getHours(), settings.getOffset());
    model.setTracks(titles != null ? titles : new ArrayList<ScheduledTrack>());
    List<ScheduledTrack> violations = new ArrayList<ScheduledTrack>();
    this.forecast.checkGVLRules(model.getTracks(), violations);
    model.setGVLViolations(violations);
  }

  private JComponent createListPanel() {
    final JXTable table = new JXTable(this.model);

    int timeWidth = ComponentFactory.getTableColumnWidthTime();
    table.getColumnModel().getColumn(Column.INDEX.ordinal()).setPreferredWidth(30);
    table.getColumnModel().getColumn(Column.INDEX.ordinal()).setMaxWidth(30);
    table.getColumnModel().getColumn(Column.TIME.ordinal()).setPreferredWidth(timeWidth);
    table.getColumnModel().getColumn(Column.TIME.ordinal()).setMaxWidth(timeWidth);
    table.getColumnModel().getColumn(Column.LENGTH.ordinal()).setPreferredWidth(timeWidth);
    table.getColumnModel().getColumn(Column.LENGTH.ordinal()).setMaxWidth(timeWidth);

    table.addHighlighter(new AbstractHighlighter() {

      @Override
      protected Component doHighlight(Component comp, ComponentAdapter adapter) {
        int row = table.convertRowIndexToModel(adapter.row);
        ScheduledTrack title = model.getTracks().get(row);
        if (title.getPlaylist().isShuffle() || title.getPlaylist().getId() == 0) {
          // comp.setFont(ComponentFactory.italicLabelFont);
          comp.setForeground(Color.GRAY);
        }
        if (model.isGVLValidationError(row)) {
          comp.setBackground(new Color(255, 230, 230));
        }
        return comp;
      }

    });

    return new JScrollPane(table);
  }

}
