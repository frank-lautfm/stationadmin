/**
 * 
 */
package de.stationadmin.base.mp3splitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;

/**
 * @author fkorf
 * 
 */
public class MP3Splitter {
  private static Logger log = LogManager.getLogger(MP3Splitter.class);
  private static int BEYOND_EOF = Integer.MAX_VALUE;

  public List<File> split(File inputFile, List<SplitPoint> splitPoints,
      File outputDir) throws IOException {
    log.info("splitting " + inputFile + " with " + splitPoints.size()
        + " split points");
    log.info("input file length: " + inputFile.length());
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    Collections.sort(splitPoints);

    LinkedList<SplitPoint> unprocessedSplitPoints = new LinkedList<SplitPoint>(
        splitPoints);
    if (unprocessedSplitPoints.getFirst().getPosition() != 0) {
      unprocessedSplitPoints.addFirst(new SplitPoint());
    }

    SplitPoint currentSplitPoint = unprocessedSplitPoints.remove();
    long nextSplit = unprocessedSplitPoints.isEmpty() ? BEYOND_EOF
        : unprocessedSplitPoints.getFirst().getPosition();
    log.info("next split at " + nextSplit);

    LinkedList<SplitPoint> processedSplitPoints = new LinkedList<SplitPoint>();

    // determine positions for split points
    log.info("resolve positions for split point");
    FileInputStream inStream = new FileInputStream(inputFile);
    Bitstream bitStream = new Bitstream(inStream);
    try {
      int firstHeaderPos = bitStream.header_pos();
      int bytes = firstHeaderPos;
      Header header = bitStream.readFrame();
      double time = 0;
      while (header != null) {
        bytes += 4 + header.framesize;
        time += header.ms_per_frame();
        bitStream.closeFrame();
        header = bitStream.readFrame();

        if (time >= nextSplit) {
          processedSplitPoints.add(currentSplitPoint);
          currentSplitPoint = unprocessedSplitPoints.remove();
          currentSplitPoint.setOffset(bytes);
          log.info("offset at " + bytes + ", time = " + (int) time);
          nextSplit = unprocessedSplitPoints.isEmpty() ? BEYOND_EOF
              : unprocessedSplitPoints.getFirst().getPosition();
          log.info("next split at " + nextSplit);
        }
      }
      processedSplitPoints.add(currentSplitPoint);
      inStream.close();
      bitStream.close();

      int fileIdx = 1;

      String baseName = FilenameUtils.concat(outputDir.getAbsolutePath(),
          FilenameUtils.getBaseName(inputFile.getName()).replaceAll("\\'",""));
      
      inStream = new FileInputStream(inputFile);

      List<File> files = new ArrayList<File>();

      currentSplitPoint = processedSplitPoints.remove();
      nextSplit = processedSplitPoints.isEmpty() ? Integer.MAX_VALUE
          : processedSplitPoints.getFirst().getOffset();
      String filenameOut = baseName + "-" + fileIdx + ".mp3";
      FileOutputStream out = new FileOutputStream(filenameOut);
      log.info("create " + filenameOut);
      fileIdx++;

      inStream.skip(firstHeaderPos);
      int bytesRed = firstHeaderPos;

      byte[] buf = new byte[4096];
      int len = inStream.read(buf);
      bytesRed += len;
      int bytesToRead = 4096;
      while (len > 0) {
        out.write(buf, 0, len);
        bytesToRead = Math.min(4096, (int) (nextSplit - bytesRed));
        if (bytesToRead > 0) {
          len = inStream.read(buf, 0, bytesToRead);
          bytesRed += len;
        } else {
          len = 0;
        }

        if (bytesRed >= nextSplit) {
          out.close();
          this.tag(filenameOut, currentSplitPoint);
          files.add(new File(filenameOut));

          currentSplitPoint = processedSplitPoints.remove();
          nextSplit = processedSplitPoints.isEmpty() ? Integer.MAX_VALUE
              : processedSplitPoints.getFirst().getOffset();

          filenameOut = baseName + "-" + fileIdx + ".mp3";
          log.info("create " + filenameOut);
          out = new FileOutputStream(filenameOut);
          fileIdx++;
        }
      }

      out.close();
      this.tag(filenameOut, currentSplitPoint);
      files.add(new File(filenameOut));

      return files;

    } catch (BitstreamException e) {
      throw new IOException(e);
    } finally {
      inStream.close();
    }

  }

  private void tag(String filename, SplitPoint splitPoint) throws IOException {
    log.info("add ID3 tags to " + filename);
    try {
      MP3File file = new MP3File(new File(filename));
      if (file.getID3V2Tag() != null) {
        file.removeID3V2Tag();
        file.sync();
      }
      ID3V2_3_0Tag tag = new ID3V2_3_0Tag();
      // ID3V1_1Tag tag = new ID3V1_1Tag();
      if (splitPoint.getArtist() != null) {
        tag.setArtist(splitPoint.getArtist());
      }
      if (splitPoint.getTitle() != null) {
        tag.setTitle(splitPoint.getTitle());
      }
      if (splitPoint.getAlbum() != null) {
        tag.setAlbum(splitPoint.getAlbum());
      }
      file.setID3Tag(tag);
      file.sync();

    } catch (ID3Exception e) {
      log.error("tagging error", e);

    }

  }

}
