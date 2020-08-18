package de.stationadmin.gui.playlist.config.shuffle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.stationadmin.base.playlist.scheduled.ScheduledItem;

public class ScheduledItemRule {
  private String id = UUID.randomUUID().toString();
  private static String[] weekdays = { "So", "Mo", "Di", "Mi", "Do", "Fr", "Sa" };
  private ScheduledItem scheduledItem;
  private int day = -1;
  private int hour = -1;
  private int minute = 0;
  private int interval = 60;

  public ScheduledItemRule() {

  }

  public ScheduledItemRule(ScheduledItem item, Map<String, Object> map) {
    this.scheduledItem = item;
    this.day = map.containsKey("day") ? (Integer)map.get("day") : -1;
    this.hour = map.containsKey("hour") ? (Integer) map.get("hour") : -1;
    this.minute = map.containsKey("minute") ? (Integer) map.get("minute") : 15;
    this.interval = map.containsKey("interval") ? (Integer) map.get("interval") : 0;
  }

  public Map<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap<>();
    if (scheduledItem != null) {
      map.put("id", scheduledItem.getId());
      scheduledItem.updateIn(map);
    }
    if (this.hour > -1) {
      map.put("hour", hour);
    } else {
      map.put("interval", this.interval);
    }
    if(this.day != -1) {
      map.put("day", this.day);
    }
    else {
      map.remove("day");
    }
    map.put("minute", this.minute);

    return map;
  }

  public ScheduledItem getScheduledItem() {
    return scheduledItem;
  }

  public void setScheduledItem(ScheduledItem scheduledItem) {
    this.scheduledItem = scheduledItem;
  }

  public int getHour() {
    return hour;
  }

  public void setHour(int hour) {
    this.hour = hour;
  }

  public int getMinute() {
    return minute;
  }

  public void setMinute(int minute) {
    this.minute = minute;
  }

  public int getInterval() {
    return interval;
  }

  public void setInterval(int interval) {
    this.interval = interval;
  }

  public String toString() {
    String name = this.scheduledItem != null ? this.scheduledItem.getName() : "?";
    String dayStr = "";
    if(this.day >= 0) {
      dayStr = weekdays[day] + " ";
    }
    else if(this.day == -2) {
      dayStr = "Mo-Fr ";
    }
    else if(this.day == -3) {
      dayStr = "Sa/So ";
    }
      
    return name + " (" + dayStr + (this.hour > -1 ? Integer.toString(hour) : "") + ":" + (this.minute > 9 ? Integer.toString(minute) : "0" + minute) + ")";
  }

  public String getId() {
    return id;
  }

  public int getDay() {
    return day;
  }

  public void setDay(int day) {
    this.day = day;
  }

}
