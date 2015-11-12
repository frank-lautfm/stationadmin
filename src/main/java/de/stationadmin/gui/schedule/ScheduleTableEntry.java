/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.awt.Color;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.schedule.Schedule.Weekday;

/**
 * @author korf
 * 
 */
public class ScheduleTableEntry {
  private Playlist playlist;
  private int hour;
  private Weekday weekday;
  private boolean playlistStart;
  private Color color = Color.WHITE;
  private Color fontColor = Color.BLACK;

  /**
   * @param playlist
   * @param hour
   * @param weekday
   */
  public ScheduleTableEntry(Playlist playlist, int hour, Weekday weekday) {
    super();
    this.playlist = playlist;
    this.hour = hour;
    this.weekday = weekday;
    String color = playlist.getId() > 0 ? playlist.getColor() : "#DDDDDD";
    if (color != null) {
      try {
        int r = Integer.parseInt(color.substring(1, 3), 16);
        int g = Integer.parseInt(color.substring(3, 5), 16);
        int b = Integer.parseInt(color.substring(5), 16);
        this.color = new Color(r, g, b);
        
        int avg = (r + g + b) / 3;
        if(avg < 125) {
          this.fontColor = Color.WHITE;
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * @return the hour
   */
  public int getHour() {
    return hour;
  }

  /**
   * @return the playlist
   */
  public Playlist getPlaylist() {
    return playlist;
  }

  /**
   * @return the weekday
   */
  public Weekday getWeekday() {
    return weekday;
  }

  /**
   * @return the playlistStart
   */
  public boolean isPlaylistStart() {
    return playlistStart;
  }

  /**
   * @param hour
   *          the hour to set
   */
  public void setHour(int hour) {
    this.hour = hour;
  }

  /**
   * @param playlist
   *          the playlist to set
   */
  public void setPlaylist(Playlist playlist) {
    this.playlist = playlist;
  }

  /**
   * @param playlistStart
   *          the playlistStart to set
   */
  public void setPlaylistStart(boolean playlistStart) {
    this.playlistStart = playlistStart;
  }

  /**
   * @param weekday
   *          the weekday to set
   */
  public void setWeekday(Weekday weekday) {
    this.weekday = weekday;
  }

  /**
   * @return the color
   */
  public Color getColor() {
    return color;
  }

  /**
   * @return the fontColor
   */
  public Color getFontColor() {
    return fontColor;
  }

  public String getRgbColor() {
    return toRGB(this.color);
  }

  public String getRgbFontColor() {
    return toRGB(this.fontColor);
  }

  private static String toRGB(Color color) {
    return "#" + to2Hex(color.getRed()) + to2Hex(color.getGreen()) + to2Hex(color.getBlue());
  }

  private static String to2Hex(int i) {
    String str = Integer.toHexString(i);
    return str.length() == 1 ? "0" + str : str;
  }

}
