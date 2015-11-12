package de.stationadmin.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

/**
 * Base class for histories (title history, listener stats history). Provides logging
 * capabilities
 *
 * @author Frank Korf
 */
public abstract class History {
  protected DateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
  private String logFile;

  public String getLogFile() {
    return logFile;
  }

  public void setLogFile(String logFile) {
    this.logFile = logFile;
  }

  protected String getEffectiveLogFile() {
    String logFile = this.logFile;
    logFile = StringUtils.replace(logFile, "%day%", this.dayFormat.format(new Date()));
    return logFile;
  }

  protected void logToFile(String str) {
    if (this.logFile != null) {
      try {
        FileWriter writer = new FileWriter(new File(this.getEffectiveLogFile()), true);
        writer.write(str);
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

}
