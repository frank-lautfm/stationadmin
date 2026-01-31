/**
 * 
 */
package de.stationadmin.base.tools;

import java.io.File;
import java.io.IOException;

import org.apache.hc.core5.annotation.Obsolete;
import org.apache.logging.log4j.LogManager;

import de.stationadmin.base.LiveAccount;
import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.Version;
import de.stationadmin.base.tasks.AbstractTask;
import de.stationadmin.base.tasks.TaskExecutionResult;
import de.stationadmin.streamlive.MP3FileSource;
import de.stationadmin.streamlive.MP3Source;
import de.stationadmin.streamlive.MP3Streamer;
import de.stationadmin.streamlive.MP3Streamer.Status;
import de.stationadmin.streamlive.MP3URLSource;


/**
 * @author korf
 * 
 */
public class MP3StreamerTask extends AbstractTask {
  private String sourceFile;
  private String metaDataFile;
  private boolean waitForTrackChange;
  private int adTriggerPosition1 = -1;
  private int adTriggerPosition2 = -1;
  private int maxDuration = 0;

  /**
   * @see de.emjoy.stationadmin.base.tasks.Task#execute(de.emjoy.stationadmin.base.StationAdminClient)
   */
  @Override
  public TaskExecutionResult execute(StationAdminClient client) {
    TaskExecutionResult result = new TaskExecutionResult();
    if (client.getMp3Streamer() != null && client.getMp3Streamer().getStatus() != Status.OFFLINE) {
      result.addMessage(true, "mp3streamer.task.msg.already_broadcasting");
      return result;
    }
    MP3Source source = null;
    if (new File(this.sourceFile).exists()) {
      source = new MP3FileSource(new File(this.sourceFile));
    } else if (this.sourceFile != null && this.sourceFile.toLowerCase().startsWith("http")) {
      source = new MP3URLSource(this.sourceFile);
    }
    if (source == null) {
      result.addMessage(true, "mp3streamer.task.msg.source_not_found");
      return result;
    }
    try {
      LiveAccount account = client.getLiveAccount();
      final MP3Streamer streamer = new MP3Streamer(source, this.metaDataFile != null ? new File(metaDataFile) : null);
      streamer.configureServer(account.getServer(), account.getPort(), client.getStation(), account.getUser(), account.getPassword());
      streamer.setUserAgent("Station Admin " + Version.VERSION + "; " + System.getProperty("os.name"));
      if(adTriggerPosition1 > -1 && adTriggerPosition2 > 0) {
        streamer.addAdTriggers(adTriggerPosition1, adTriggerPosition2);
      }
      client.setMp3Streamer(streamer);

      StreamerThread t = new StreamerThread(streamer);
      t.start();
      
      long ts = System.currentTimeMillis();
      int rc = -1;
      do {
      	try {
      	Thread.sleep(500);
      	} catch(Exception e) {
      	}
      	rc = streamer.getReturnCode();
      } while(rc < 0 && (System.currentTimeMillis() - ts) < 60000);
      
      if (rc == 401 || rc == 403) {
        result.addMessage(true, "mp3streamer.task.msg.authentication_error");
      } else if (rc != 200) {
        result.addMessage(true, "mp3streamer.task.msg.connection_error", Integer.toString(rc));
      }
      else {
      	result.addMessage(false, "mp3streamer.task.msg.started", Integer.toString(rc));
      }

    } catch (IOException e) {
      result.addMessage(true, "mp3streamer.task.msg.error", e.getMessage() != null ? e.getMessage() : e.toString());
    }
    return result;
  }

  /**
   * @return the sourceFile
   */
  public String getSourceFile() {
    return sourceFile;
  }

  /**
   * @param sourceFile
   *          the sourceFile to set
   */
  public void setSourceFile(String sourceFile) {
    this.sourceFile = sourceFile;
  }

  /**
   * @return the metaDataFile
   */
  public String getMetaDataFile() {
    return metaDataFile;
  }

  /**
   * @param metaDataFile
   *          the metaDataFile to set
   */
  public void setMetaDataFile(String metaDataFile) {
    this.metaDataFile = metaDataFile;
  }

  /**
   * @return the waitForTrackChange
   */
  @Obsolete
  public boolean isWaitForTrackChange() {
    return waitForTrackChange;
  }

  /**
   * @param waitForTrackChange
   *          the waitForTrackChange to set
   */
  @Obsolete
  public void setWaitForTrackChange(boolean waitForTrackChange) {
    this.waitForTrackChange = waitForTrackChange;
  }

  private static class StreamerThread extends Thread {
    private MP3Streamer streamer;
    
    /**
     * @param streamer
     * @param waitForTrackChange
     */
    public StreamerThread(MP3Streamer streamer) {
      super();
      this.setDaemon(true);
      this.setName("MP3 Streamer");
      this.streamer = streamer;
    }

    public void run() {
      try {
        streamer.run();
      } catch (Exception e) {
        LogManager.getLogger(MP3StreamerTask.class).error("unable to start broadcast", e);
      }

    }
  }

  /**
   * @return the maxDuration
   */
  public int getMaxDuration() {
    return maxDuration;
  }

  /**
   * @param maxDuration the maxDuration to set
   */
  public void setMaxDuration(int maxDuration) {
    this.maxDuration = maxDuration;
  }

  public int getAdTriggerPosition1() {
    return adTriggerPosition1;
  }

  public void setAdTriggerPosition1(int adTriggerPosition1) {
    this.adTriggerPosition1 = adTriggerPosition1;
  }

  public int getAdTriggerPosition2() {
    return adTriggerPosition2;
  }

  public void setAdTriggerPosition2(int adTriggerPosition2) {
    this.adTriggerPosition2 = adTriggerPosition2;
  }

}
