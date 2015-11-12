/**
 * 
 */
package de.stationadmin.gui.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.util.AbstractBean;

/**
 * 
 * @author Frank Korf
 * 
 */
public class UploadManager extends AbstractBean {
  private static final Logger log = Logger.getLogger(UploadManager.class);
  private StationAdminClient client;
  private UploadProgressListener progressListener = new UploadProgressListener();
  private List<File> files = Collections.synchronizedList(new ArrayList<File>());
  private volatile int currentIndex = 0;
  private volatile boolean running = false;
  private volatile boolean stop = false;

  public UploadManager(StationAdminClient client) {
    super();
    this.client = client;
    this.loadQueue();
  }

  public boolean add(File file) {
    if (file.exists() && !file.isDirectory() && file.getName().toLowerCase().endsWith("mp3")
        && file.length() < 1024 * 1024 * 25) {
      int oldRemaining = this.getNumberOfRemainingFiles();
      this.files.add(file);
      this.progressListener.add(file);
      this.firePropertyChange("numberOfRemainingFiles", oldRemaining, this.getNumberOfRemainingFiles());
      this.saveQueue();
      return true;
    } else {
      return false;
    }
  }

  public boolean removeFile(File file) {
    if (this.currentIndex < this.files.size()
        && this.files.get(currentIndex).getAbsolutePath().equals(file.getAbsolutePath())) {
      this.progressListener.setAbortCurrent(true);
    }
    int oldRemaining = this.getNumberOfRemainingFiles();
    int startIndex = this.running ? this.currentIndex + 1 : this.currentIndex;
    for (int i = startIndex; i < this.files.size(); i++) {
      if (this.files.get(i).getAbsolutePath().equals(file.getAbsolutePath())) {
        this.files.remove(i);
        this.progressListener.remove(file);
        this.firePropertyChange("numberOfRemainingFiles", oldRemaining, this.getNumberOfRemainingFiles());
        this.saveQueue();
        return true;
      }
    }
    return false;
  }

  public int getNumberOfRemainingFiles() {
    return this.files.size() - this.currentIndex;
  }

  public void run() throws IOException {
    this.setRunning(true);
    this.stop = false;
    try {
      while (this.currentIndex < this.files.size() && !stop) {
        try {
          this.progressListener.setAbortCurrent(false);
          this.client.getTrackService().upload(files.get(this.currentIndex), progressListener);
        } catch (InterruptedIOException e) {
          log.info("upload interrupted");
        }
        this.progressListener.currentUploadCompleted();
        int oldRemaining = this.getNumberOfRemainingFiles();
        this.currentIndex++;
        this.firePropertyChange("numberOfRemainingFiles", oldRemaining, this.getNumberOfRemainingFiles());
        this.saveQueue();
      }
      this.progressListener.reset();
    } finally {
      this.setRunning(false);
    }
  }

  private void saveQueue() {
    List<String> list = new ArrayList<String>();
    for (int i = this.currentIndex; i < this.files.size(); i++) {
      list.add(this.files.get(i).getAbsolutePath());
    }
    String filename = this.client.getSessionCtx().getStationDirectory() + "uploadqueue.txt";
    try {
      FileOutputStream out = new FileOutputStream(new File(filename));
      IOUtils.writeLines(list, null, out, "UTF-8");
      IOUtils.closeQuietly(out);
    } catch (IOException e) {
      log.error("unable to write upload queue file", e);
    }
  }

  @SuppressWarnings("unchecked")
  private void loadQueue() {
    String filename = this.client.getSessionCtx().getStationDirectory() + "uploadqueue.txt";
    if (new File(filename).exists()) {
      try {
        FileInputStream in = new FileInputStream(filename);
        List<String> lines = (List<String>) IOUtils.readLines(in, "UTF-8");
        IOUtils.closeQuietly(in);
        int old = this.files.size();
        for (String line : lines) {
          File file = new File(line);
          if (file.exists()) {
            this.files.add(file);
            this.progressListener.add(file);
          }
        }
        this.firePropertyChange("numberOfRemainingFiles", old, this.files.size());
      } catch (IOException e) {
        log.error("unable to write upload queue file", e);
      }

    }

  }

  private void setRunning(boolean running) {
    boolean old = this.running;
    this.running = running;
    this.firePropertyChange("running", old, running);
  }

  public void stop() {
    this.stop = true;
  }

  /**
   * @return the progressListener
   */
  public UploadProgressListener getProgressListener() {
    return progressListener;
  }

  /**
   * @return the files
   */
  public List<File> getFiles() {
    return Collections.unmodifiableList(files);
  }

  /**
   * @return the currentIndex
   */
  public int getCurrentIndex() {
    return currentIndex;
  }

  /**
   * @return the running
   */
  public boolean isRunning() {
    return running;
  }

  /**
   * @return the client
   */
  public StationAdminClient getClient() {
    return client;
  }

}
