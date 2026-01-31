/**
 * 
 */
package de.stationadmin.streamlive;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;

import de.stationadmin.lfmapi.LautfmService;

/**
 * @author korf
 * 
 */
public class MP3Streamer {
  private IcecastServerConnector ice;
  private MP3Writer mp3Writer;
  private MP3Source source;
  private MetaDataWriter metaWriter;
  private String station;
  private Status status = Status.OFFLINE;
  private File meta;
  private int maxDuration = 0;
  private volatile int connectReturnCode = -1;

  public MP3Streamer(File sourcefile) throws IOException {
    this(sourcefile, null);
  }

  public MP3Streamer(MP3Source source, File metaFile) throws IOException {
    ice = new IcecastServerConnector();
    this.source = source;
    if (metaFile != null && metaFile.exists()) {
      this.meta = metaFile;
      this.metaWriter = new MetaDataWriter(metaFile, ice);
    }
  }

  public MP3Streamer(File sourcefile, File metaFile) throws IOException {
    ice = new IcecastServerConnector();
    this.source = new MP3FileSource(sourcefile);
    if (metaFile == null) {
      metaFile = new File(FilenameUtils.removeExtension(sourcefile.getAbsolutePath()) + ".txt");

    }
    if (metaFile.exists()) {
      this.meta = metaFile;
      this.metaWriter = new MetaDataWriter(metaFile, ice);
    }
  }

  public void abort() {
    if (this.mp3Writer != null) {
      this.mp3Writer.abort();
    }
    if (this.metaWriter != null) {
      this.metaWriter.abort();
    }
  }

  public int getPlayTime() {
    return this.mp3Writer != null && this.status != Status.OFFLINE && this.mp3Writer.getStartTime() > 0
        ? (int) ((System.currentTimeMillis() - this.mp3Writer.getStartTime()) / 1000)
        : 0;
  }

  public int getNumTracks() {
    return this.metaWriter != null ? this.metaWriter.getSongs().size() : 0;
  }

  public int getCurrentTrackIndex() {
    return this.metaWriter != null && this.metaWriter.getCurrentSong() != null ? this.metaWriter.getSongs().indexOf(this.metaWriter.getCurrentSong()) : -1;
  }

  public String getCurrentSong() {
    return this.metaWriter != null && this.status != Status.OFFLINE ? this.metaWriter.getCurrentSong() : "";
  }

  public void configureServer(String host, int port, String station, String user, String password) {
    this.station = station;
    this.ice.setHostName(host);
    this.ice.setPort(port);
    this.ice.setMountPoint(station);
    this.ice.setUserName(user);
    this.ice.setPassword(password);
    this.ice.setSourceName(station);
    this.ice.setSourceDescription(station);
  }

  public void setUserAgent(String userAgent) {
    this.ice.setUserAgent(userAgent);
  }

  public void setBufferTime(int time) {
    this.mp3Writer.setMaxBuffer(time);
  }

  public int getBufferTime() {
    return this.mp3Writer.getMaxBuffer();
  }

  public int testConnect() throws IOException {
    int rc = this.ice.connect();
    this.ice.disconnect();
    return rc;
  }

  public boolean addAdTriggers(int pos1, int pos2) {
    try {
      if (this.metaWriter == null) {
        this.metaWriter = new MetaDataWriter(null, ice);
      }
      return this.metaWriter.addAdTriggers(station, pos1, pos2);
    } catch (Exception e) {
      return false;
    }
  }

  public void run() throws IOException {
    try {
      this.connectReturnCode = this.ice.connect();
      if (this.connectReturnCode == 200) {
        this.mp3Writer = new MP3Writer(source.getInputStream(), this.ice.getOutStream(), this.metaWriter);
        this.mp3Writer.setMaxDuration(this.maxDuration);
        /*
         * if (this.metaWriter != null) { Thread metaWriterThread = new Thread() {
         * public void run() { try { Thread.sleep(5000); // allow some time to connect
         * to Icecast while (mp3Writer.getStartTime() == 0 && !metaWriter.isAbort()) {
         * Thread.sleep(500); } metaWriter.write(mp3Writer.getStartTime()); } catch
         * (Exception e) { e.printStackTrace(); } } }; metaWriterThread.start(); }
         */
        try {
          this.status = Status.ONLINE;
          this.mp3Writer.write();
        } finally {
          this.ice.disconnect();
          if (this.metaWriter != null) {
            this.metaWriter.abort();
          }
        }
      } else {
        throw new IOException("Icecast connection failed with status " + this.connectReturnCode);
      }
    } finally {
      this.status = Status.OFFLINE;

    }
  }
  
  public int getReturnCode() {
  	return this.connectReturnCode;
  }
  

  public enum Status {
    OFFLINE, WAITING, ONLINE

  }

  /**
   * @return the source
   */
  public MP3Source getSource() {
    return source;
  }

  /**
   * @return the meta
   */
  public File getMeta() {
    return meta;
  }

  /**
   * @return the status
   */
  public Status getStatus() {
    return status;
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

}
