/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.awt.Color;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import javax.swing.table.AbstractTableModel;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.base.schedule.Schedule.Weekday;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class ScheduleTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -6946723823350337471L;

  // private ClientContext ctx;

  private PlaylistRegistry playlistRegistry;
  private TextProvider textProvider;
  private Schedule schedule;

  private List<Schedule.Entry> schedulerEntries;
  private ScheduleTableEntry[][] entries;
  private ValueModel modified = new ValueHolder(false);

  /**
   * @param ctx
   */
  public ScheduleTableModel(TextProvider textProvider, PlaylistRegistry playlistRegistry, Schedule schedule) {
    super();
    this.textProvider = textProvider;
    this.playlistRegistry = playlistRegistry;
    this.schedule = schedule;
    this.schedulerEntries = new ArrayList<Schedule.Entry>(this.schedule.getEntries());
    this.rebuildEntries();
  }

  /**
   * Restores the model to the state that is stored in {@link Schedule}
   */
  public void reset() {
    this.schedulerEntries = new ArrayList<Schedule.Entry>(this.schedule.getEntries());
    this.rebuildEntries();
    this.modified.setValue(false);
  }

  public void clear() {
    this.schedulerEntries.clear();
    for (int i = 0; i < 7; i++) {
      this.schedulerEntries.add(new Schedule.Entry(schedule.getBasePlaylist().getId(), Weekday.values()[i], 0));
    }
    this.rebuildEntries();
    this.modified.setValue(true);
  }

  public void setEntries(List<Schedule.Entry> entries) {
    if (entries.size() > 0) {
      this.schedulerEntries.clear();
      this.schedulerEntries.addAll(entries);
      this.rebuildEntries();
    } else {
      this.clear();
    }
    this.modified.setValue(true);
  }

  /**
   * Saves the values of the model to {@link Schedule}
   */
  public void commit() {
    this.schedule.clear();
    for (Schedule.Entry entry : schedulerEntries) {
      this.schedule.addEntry(entry);
    }
    this.modified.setValue(false);
  }

  private void rebuildEntries() {
    this.entries = new ScheduleTableEntry[7][];
    for (int i = 0; i < 7; i++) {
      this.entries[i] = new ScheduleTableEntry[24];
    }

    ScheduleTableEntry last = new ScheduleTableEntry(this.schedule.getBasePlaylist(), 0, Weekday.MONDAY);
    last.setPlaylistStart(true);

    // create start entries
    for (Schedule.Entry entry : this.schedulerEntries) {
      Playlist playlist = this.playlistRegistry.getPlaylist(entry.getPlaylistId());
      if (playlist != null) {
        ScheduleTableEntry tableEntry = new ScheduleTableEntry(playlist, entry.getHour(), entry.getWeekday());
        tableEntry.setPlaylistStart(true);
        this.entries[tableEntry.getWeekday().getRawDay() - 1][tableEntry.getHour()] = tableEntry;
        if (tableEntry.getWeekday().ordinal() > last.getWeekday().ordinal()
            || (tableEntry.getWeekday() == last.getWeekday() && tableEntry.getHour() > last.getHour())) {
          last = tableEntry;
        }
      }
    }

    // create follow-up entries
    for (int day = 0; day < 7; day++) {
      for (int hour = 0; hour < 24; hour++) {
        if (this.entries[day][hour] != null) {
          last = this.entries[day][hour];
        } else {
          ScheduleTableEntry tableEntry = new ScheduleTableEntry(last.getPlaylist(), hour, Weekday.values()[day]);
          this.entries[tableEntry.getWeekday().getRawDay() - 1][tableEntry.getHour()] = tableEntry;
        }
      }
    }

    this.fireTableDataChanged();
  }

  public ScheduleTableEntry getEntryAt(int day, int hour) {
    return this.entries[day][hour];
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return 24;
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (columnIndex == 0) {
      return rowIndex;
    } else {
      ScheduleTableEntry entry = this.entries[columnIndex - 1][rowIndex];
      if (entry != null && entry.isPlaylistStart() && entry.getPlaylist().getId() > 0) {
        return entry.getPlaylist().getDisplayName();
      } else {
        return "";
      }
    }
  }

  public void removeEntry(ScheduleTableEntry entry) {
    Schedule.Entry e = new Schedule.Entry(entry.getPlaylist().getId(), entry.getWeekday(), entry.getHour());
    this.schedulerEntries.remove(e);
    this.rebuildEntries();
    this.modified.setValue(true);
  }

  public void setPlaylistAt(Playlist playlist, int day, int hour) {
    if (this.entries[day][hour].isPlaylistStart()) {
      Schedule.Entry e = new Schedule.Entry(this.entries[day][hour].getPlaylist().getId(),
          this.entries[day][hour].getWeekday(), this.entries[day][hour].getHour());
      this.schedulerEntries.remove(e);
    }
    Schedule.Entry e = new Schedule.Entry(playlist.getId(), Weekday.values()[day], hour);
    this.schedulerEntries.add(e);
    Collections.sort(this.schedulerEntries);
    int idx = this.schedulerEntries.indexOf(e);
    if (idx < this.schedulerEntries.size() - 1) {
      Schedule.Entry next = this.schedulerEntries.get(idx + 1);
      if(next.getPlaylistId() == e.getPlaylistId() && next.getWeekday() == e.getWeekday()) {
        // next entry refers to same playliste - remove
        this.schedulerEntries.remove(idx + 1);
      }
    }

    this.rebuildEntries();
    this.modified.setValue(true);

  }

  private static String toRGB(Color color) {
    return "#" + to2Hex(color.getRed()) + to2Hex(color.getGreen()) + to2Hex(color.getBlue());
  }

  private static String to2Hex(int i) {
    String str = Integer.toHexString(i);
    return str.length() == 1 ? "0" + str : str;
  }

  public String toHtml(String template) throws Exception {
    Properties props = new Properties();
    props.put("resource.loader", "class");
    props.put("class.resource.loader.description", "Velocity Classpath Resource Loader");
    props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    VelocityEngine engine = new VelocityEngine(props);

    if (!template.endsWith(".vm")) {
      template = template + ".vm";
    }
    Template t = engine.getTemplate(template);
    VelocityContext ctx = new VelocityContext();
    ctx.put("entries", this.entries);
    ctx.put("marked", new BitSet());

    StringWriter writer = new StringWriter();
    t.merge(ctx, writer);

    return writer.toString();
  }

  public String toHtml(boolean colorizePlaylists) {
    StringBuilder html = new StringBuilder();

    html.append("<style>\n");
    html.append("  table.schedule { \n");
    html.append("    border-spacing:1px;\n");
    html.append("    font-family:Arial,Helvectica;\n");
    html.append("    font-size:9pt;\n");
    html.append("  }\n");
    html.append("  td.head { background:#CCCCCC; padding:5px; }\n");
    html.append("  td.hour { background:#CCCCCC; padding:5px; }\n");
    HashSet<Playlist> declared = new HashSet<Playlist>();
    for (int day = 0; day < 7; day++) {
      for (int hour = 0; hour < 24; hour++) {
        Playlist playlist = this.entries[day][hour].getPlaylist();
        if (!declared.contains(playlist)) {
          String color = colorizePlaylists && playlist.getId() != 0
              ? toRGB(this.entries[day][hour].getColor())
              : "#FFFFFF";
          String fontColor = colorizePlaylists && playlist.getId() != 0
              ? toRGB(this.entries[day][hour].getFontColor())
              : "#000000";
          html.append("  td.pl" + playlist.getId() + " { ");
          html.append("background:" + color + "; color:" + fontColor + "; padding:5px; vertical-align:top } /* "
              + playlist.getDisplayName() + " */\n");
          declared.add(playlist);
        }
      }
    }
    html.append("</style>\n");

    html.append("<table class='schedule'>\n");
    html.append("<tr>\n");
    html.append("<td class=\"head\" width='5%'>" + textProvider.getString("scheduleeditor.hour") + "</td>");
    for (int i = 0; i < 7; i++) {
      html.append("<td class=\"head\" width='13%'>"
          + textProvider.getString("weekday." + Weekday.values()[i].name().toLowerCase()) + "</td>");
    }
    html.append("</tr>\n");

    for (int hour = 0; hour < 24; hour++) {
      html.append("<tr>\n");
      html.append("<td class=\"hour\">" + hour + "</td>\n");
      for (int day = 0; day < 7; day++) {
        if (entries[day][hour].isPlaylistStart()) {
          int rowSpan = 1;
          for (int t = hour + 1; t < 24 && !entries[day][t].isPlaylistStart(); t++) {
            rowSpan++;
          }
          Playlist playlist = entries[day][hour].getPlaylist();
          String name = playlist.getId() > 0 ? playlist.getDisplayName() : "";
          html.append("<td class=\"pl" + playlist.getId() + "\" rowspan='" + rowSpan + "'>" + name + "</td>\n");
        }
      }

      html.append("</tr>\n");
    }
    html.append("</table>\n");

    return html.toString();
  }

  public enum Column {
    HOUR, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
  }

  /**
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    if (column == 0) {
      return textProvider.getString("scheduleeditor.hour");

    } else {
      return textProvider.getString("weekday." + Column.values()[column].name().toLowerCase());
    }
  }

  /**
   * @return the modified
   */
  public ValueModel getModified() {
    return modified;
  }

}
