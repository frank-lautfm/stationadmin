package de.stationadmin.base.util;

import java.text.NumberFormat;

public class TimeFormat {

  public static String format(int seconds, boolean withHours) {
    int hours = seconds / (60 * 60);
    int remaining = seconds % (60 * 60);
    int minutes = remaining / 60;
    int sec = remaining % 60;

    NumberFormat fmt = NumberFormat.getIntegerInstance();
    fmt.setMinimumIntegerDigits(2);
    if (withHours) {
      return fmt.format(hours) + ":" + fmt.format(minutes) + ":" + fmt.format(sec);
    } else {
      return fmt.format(minutes) + ":" + fmt.format(sec);
    }

  }
}
