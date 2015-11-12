/**
 * 
 */
package de.stationadmin.base.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import de.stationadmin.base.Service;
import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.util.AbstractBean;

/**
 * @author korf
 * 
 */
public class TaskExecutionService extends AbstractBean implements Service {
  private static final Logger log = Logger.getLogger(TaskExecutionService.class);
  private XStream xstream;
  private List<ScheduledTask> scheduledTasks = new ArrayList<ScheduledTask>();
  private StationAdminClient client;
  private TaskLauncher taskLauncher = new TaskLauncher();
  private Timer timer;

  public TaskExecutionService(StationAdminClient client) {
    super();
    this.xstream = new XStream(new DomDriver("UTF-8"));
    this.client = client;
    new File(this.getTaskDir()).mkdirs();

  }

  @Override
  public void load() throws IOException {
    File dir = new File(this.getTaskDir());
    File[] files = dir.listFiles();
    this.scheduledTasks.clear();
    if (files != null) {
      log.info("loading scheduled tasks");
      for (File file : files) {
        if (file.getName().endsWith(".task")) {
          try {
            FileInputStream in = new FileInputStream(file);
            ScheduledTask stask = this.load(in);
            IOUtils.closeQuietly(in);
            this.scheduledTasks.add(stask);
          } catch (Exception e) {
            log.error("unable to load task from " + file.getName(), e);
          }
        }
      }
      log.info("found " + this.scheduledTasks.size() + " scheduled tasks");
    }
  }
  
  public ScheduledTask load(InputStream stream) throws IOException {
    return (ScheduledTask) this.xstream.fromXML(stream);
  }
  
  public List<File> getTaskFiles() {
    List<File> taskFiles = new ArrayList<File>();
    File dir = new File(this.getTaskDir());
    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.getName().endsWith(".task")) {
          taskFiles.add(file);
        }
      }
    }
    return taskFiles;
  }

  @Override
  public void synchronize() throws IOException {
    // nothing to do
  }

  @Override
  public void close() {
    if (this.taskLauncher != null) {
      this.taskLauncher.cancel();
      this.taskLauncher = null;
    }
    if (this.timer != null) {
      this.timer.cancel();
      this.timer = null;
    }
  }

  @Override
  public void initBackgroundTasks() {
    this.timer = new Timer(true);
    timer.schedule(this.taskLauncher, 1000 * 60, 1000 * 60);
  }

  public List<ScheduledTask> getScheduledTasks() {
    return Collections.unmodifiableList(scheduledTasks);
  }

  private String getTaskDir() {
    return this.client.getSessionCtx().getStationDirectory() + File.separatorChar + "tasks" + File.separatorChar;
  }

  public void configureScheduledTask(ScheduledTask stask) throws IOException {
    this.save(stask);
    boolean found = false;
    for (int i = 0; i < this.scheduledTasks.size() && !found; i++) {
      if (scheduledTasks.get(i).getId().equals(stask.getId())) {
        scheduledTasks.set(i, stask);
        found = true;
      }
    }
    if (!found) {
      this.scheduledTasks.add(stask);
    }
    this.firePropertyChange("scheduledTasks", new ArrayList<ScheduledTask>(), this.scheduledTasks);
  }

  /**
   * Deletes the scheduled task with the given id
   * 
   * @param id
   * @throws IOException
   */
  public void deleteScheduledTask(String id) throws IOException {
    ArrayList<ScheduledTask> old = new ArrayList<ScheduledTask>(this.scheduledTasks);
    ScheduledTask task = null;
    for (ScheduledTask t : this.scheduledTasks) {
      if (t.getId().equals(id)) {
        task = t;
      }
    }
    this.scheduledTasks.remove(task);
    new File(this.getTaskDir() + task.getId() + ".task").delete();
    this.firePropertyChange("scheduledTasks", old, this.scheduledTasks);
  }

  /**
   * Executes the scheduled task with the given id immediately
   * 
   * @param id
   * @throws IOException
   */
  public void executeScheduledTask(String id) throws IOException {
    for (ScheduledTask t : this.scheduledTasks) {
      if (t.getId().equals(id)) {
        this.execute(t);
        return;
      }
    }
  }

  private void execute(ScheduledTask stask) throws IOException {
    long t = System.currentTimeMillis();
    TaskExecutionResult result = stask.getTask().execute(this.client);
    stask.setLastExecution(t);
    stask.setLastResult(result);
    this.save(stask);
    this.firePropertyChange("scheduledTasks", new ArrayList<ScheduledTask>(), this.scheduledTasks);
  }

  private void save(ScheduledTask task) throws IOException {
    String filename = this.getTaskDir() + task.getId() + ".task";
    FileOutputStream out = new FileOutputStream(filename);
    this.xstream.toXML(task, out);
    IOUtils.closeQuietly(out);
  }

  private void launchDueTasks() {
    List<ScheduledTask> list = new ArrayList<ScheduledTask>(this.scheduledTasks);
    for (ScheduledTask stask : list) {
      if (stask.isDue()) {
        try {
          // TODO own thread?
          log.info("execute " + stask.getTask().getClass().getSimpleName() + " " + stask.getId());
          this.execute(stask);
        } catch (IOException e) {
          log.error("task execution error", e);
        }
      }
    }

  }

  private class TaskLauncher extends TimerTask {
    private boolean active = false;

    @Override
    public void run() {
      if (!active) {
        active = true;
        try {
          launchDueTasks();
        } finally {
          active = false;
        }

      }
    }

  }

}
