/**
 * 
 */
package de.stationadmin.base.schedule;

import java.io.File;
import java.io.FileInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.tasks.AbstractTask;
import de.stationadmin.base.tasks.TaskExecutionMessage;
import de.stationadmin.base.tasks.TaskExecutionResult;

/**
 * @author korf
 * 
 */
public class ScheduleImportTask extends AbstractTask {
  private static final Logger log = LogManager.getLogger(ScheduleImportTask.class);
  private String filename;

  @Override
  public TaskExecutionResult execute(StationAdminClient client) {
    log.info("load schedule from " + this.filename);
    TaskExecutionResult result = new TaskExecutionResult();
    if (this.filename == null) {
      result.setSucceeded(false);
      result.addMessage(new TaskExecutionMessage(false, "task.configurationerror"));
    }
    if (this.filename != null && new File(this.filename).exists()) {

      try {
        FileInputStream stream = new FileInputStream(filename);
        client.getSchedule().load(stream);
        stream.close();
        client.getSchedule().submitToServer();
        client.getSchedule().save();
        result.setSucceeded(true);
        result.addMessage(new TaskExecutionMessage(false, "task.schedule.import.success", filename));
      } catch (Exception e) {
        log.error("loading of schedule failed", e);
        result.setSucceeded(false);
        result.addMessage(new TaskExecutionMessage(false, "task.schedule.import.failed", filename, e.getMessage() != null ? e.getMessage() : e
            .toString()));
      }
    } else {
      log.error("file not found: " + this.filename);
      result.setSucceeded(false);
      result.addMessage(new TaskExecutionMessage(false, "task.schedule.import.file_not_found", filename));
    }
    return result;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }
}
