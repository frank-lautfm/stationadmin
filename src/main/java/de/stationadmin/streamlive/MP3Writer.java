/**
 * 
 */
package de.stationadmin.streamlive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import de.stationadmin.base.util.TimeFormat;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;

/**
 * Writes an mp3 stream from the given source to the target.
 * <p>
 * Writing is slowed down to avoid that much more data is sent than can be
 * played in the time since the start. Only a limited buffer is allowed for
 * sending data ahead of the real time.
 * 
 * @author korf
 */
public class MP3Writer {
  private static final Logger log = Logger.getLogger(MP3Writer.class);
  private InputStream source;
  private OutputStream target;
  private int maxBuffer = 15;
  private int maxDuration = 0;
  private volatile long startTime;

  private volatile boolean abort = false;
  
  private MetaDataWriter metaDataWriter;

  public MP3Writer(File source, OutputStream target, MetaDataWriter metaDataWriter) throws IOException {
    this(new FileInputStream(source), target, metaDataWriter);
  }

  /**
   * @param source
   * @param target
   */
  public MP3Writer(InputStream source, OutputStream target, MetaDataWriter metaDataWriter) {
    super();
    this.source = source;
    this.target = target;
    this.metaDataWriter = metaDataWriter;
  }

  public void write() throws IOException {
    this.startTime = System.currentTimeMillis();
    
    if(this.metaDataWriter != null) {
      this.metaDataWriter.onStartBroadcasting();
    }

    InputStreamWrapper stream = new InputStreamWrapper();

    try {
      Bitstream bitStream = new Bitstream(stream);
      Header header = bitStream.readFrame();
      double time = 0;
      int nextReport = 15000;
      int maxtime = this.maxDuration == 0 ? Integer.MAX_VALUE : this.maxDuration * 1000 * 60;
      while (header != null && !abort && time < maxtime) {
        // bytes += 4 + header.framesize;111
        time += header.ms_per_frame();
        bitStream.closeFrame();
        header = bitStream.readFrame();
        
        if(this.metaDataWriter != null) {
          this.metaDataWriter.onTimeChange(time);
        }

        boolean wait = false;
        while (startTime + time > System.currentTimeMillis() + this.maxBuffer * 1000) {
          if (!wait) {
            if (log.isTraceEnabled()) {
              log.trace(TimeFormat.format((int) ((System.currentTimeMillis() - startTime) / 1000), false) + " "
                  + TimeFormat.format((int) (time / 1000), false) + " waiting...");
            }
          }

          if (log.isTraceEnabled()) {
            if (time > nextReport) {
              long diff = (long) (startTime + time) - System.currentTimeMillis();
              log.trace("mp3 stream buffer: " + diff + " ms");
              nextReport = (int) time + 15000;
            }
          }

          wait = true;
          try {
            Thread.sleep(250);
          } catch (Exception e) {
          }
        }
        if (wait && log.isTraceEnabled()) {
          log.trace("proceeding");
        }

      }
    } catch (BitstreamException e) {
      throw new IOException(e);
    }
  }

  private class InputStreamWrapper extends InputStream {

    @Override
    public int read() throws IOException {
      int b = source.read();
      target.write(b);
      return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
      int len = source.read(b);
      target.write(b, 0, len);
      return len;
    }

    @Override
    public int available() throws IOException {
      return source.available();
    }

    @Override
    public void close() throws IOException {
      source.close();
    }

  }

  public void abort() {
    this.abort = true;
    if(this.metaDataWriter != null) {
      this.metaDataWriter.abort();
    }
  }

  /**
   * @return the maxBuffer
   */
  public int getMaxBuffer() {
    return maxBuffer;
  }

  /**
   * @param maxBuffer
   *          the maxBuffer to set
   */
  public void setMaxBuffer(int maxBuffer) {
    this.maxBuffer = maxBuffer;
  }

  /**
   * @return the startTime
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * @param startTime
   *          the startTime to set
   */
  protected void setStartTime(long startTime) {
    this.startTime = startTime;
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
