/**
 * 
 */
package de.stationadmin.gui.loganalyzer.listeners;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JPanel;

import de.stationadmin.gui.TextProvider;

public class ListenersChart extends JPanel {
  private static final long serialVersionUID = -4001022214780734358L;
  private long offset;
  private int[] minutes;
  private int[] listenersAtX;
  private int[] minuteAtX;
  private int peak;

  private String listenersStr;
  private SimpleDateFormat timeFormat;
  private BitSet highlightedMinutes = new BitSet();

  public ListenersChart(TextProvider textProvider) {
    this.listenersStr = textProvider.getString("currentListeners");
    this.timeFormat = new SimpleDateFormat(textProvider.getString("extTimeFormat"));
    this.setToolTipText(listenersStr);
  }

  public ListenersChart(TextProvider textProvider, long offset, int[] minutes, int peak) {
    this(textProvider);
    this.update(offset, minutes, peak);

  }

  @Override
  public void paint(Graphics g) {
    if (this.minutes == null) {
      return;
    }

    Graphics2D g2d = (Graphics2D) g;
    Dimension dim = this.getSize();

    g2d.setColor(Color.white);
    g2d.fillRect(0, 0, dim.width, dim.height);

    g2d.setColor(Color.red);
    Font myFont = new Font("Arial", Font.PLAIN, 10);
    g2d.setFont(myFont);

    float xFactor = (float) minutes.length / (dim.width - 24);
    float yFactor = (float) dim.height / peak;

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(this.offset);

    int x = 0;
    int lastHour = cal.get(Calendar.HOUR_OF_DAY);
    this.listenersAtX = new int[dim.width];
    this.minuteAtX = new int[dim.width];
    for (int i = 0; i < dim.width; i++) {
      int minute = (int) (x * xFactor);
      cal.setTimeInMillis(this.offset + minute * 60000);
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      if (hour != lastHour) {
        if (i > 0) {
          this.listenersAtX[i] = this.listenersAtX[i - 1];
          this.minuteAtX[i] = this.minuteAtX[i - 1];
        }
        i++;
        lastHour = hour;
        g2d.setColor(Color.black);
        g2d.drawString(Integer.toString(hour), i, dim.height - 2);
        g2d.setColor(Color.red);
      }

      if (this.highlightedMinutes.get(minute)) {
        g2d.setColor(Color.yellow);
      } else {
        g2d.setColor(Color.red);
      }

      if (i < dim.width) {
        int idx = Math.min(minutes.length - 1, minute);
        int listeners = minutes[idx];
        this.listenersAtX[i] = listeners;
        this.minuteAtX[i] = minute;
        int y = (int) (listeners * yFactor);
        g2d.drawLine(i, dim.height - 20, i, dim.height - y - 20);
        x++;
      }

    }

  }

  @Override
  public String getToolTipText(MouseEvent event) {
    int x = event.getX();
    if (this.listenersAtX != null && x < this.listenersAtX.length) {
      Date date = new Date(this.offset + minuteAtX[x] * 60000);
      return "<html>" + Integer.toString(this.listenersAtX[x]) + " " + this.listenersStr + "<br>" + timeFormat.format(date) + "</html>";
    } else {
      return super.getToolTipText();
    }
  }

  public void highlight(long time) {
    long diff = time - offset;
    int min = (int) (diff / 60000);
    this.highlightedMinutes.clear();
    for (int i = min; i < min + 10; i++) {
      this.highlightedMinutes.set(i);
    }
    repaint();
  }

  public void clearHighlight() {
    this.highlightedMinutes.clear();
    this.repaint();
  }

  public void update(long offset, int[] valuesByMinute, int peak) {
    this.offset = offset;
    this.minutes = valuesByMinute;

    if (peak > 500) {
      this.peak = peak;
    } else if (peak >= 250) {
      this.peak = 500;
    } else if (peak >= 100) {
      this.peak = 250;
    } else if (peak >= 30) {
      this.peak = 100;
    } else if (peak >= 8) {
      this.peak = 30;
    } else {
      this.peak = 10;
    }
    this.repaint();
  }

}