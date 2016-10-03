/**
 * 
 */
package de.stationadmin.base.playlist.trackimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.trackimport.TrackImportTask.Status;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.tag.TagSet;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.base.track.format.ArtistTracknameFormat;
import de.stationadmin.base.track.format.CSVFormat;
import de.stationadmin.base.track.format.ExtendedTrackFormat;
import de.stationadmin.base.track.format.TrackExportFormat;

/**
 * Tool class for adding titles from files to a playlist
 * 
 * @author Frank Korf
 */
public class TrackImportHandler {
  private static final Logger log = Logger.getLogger(TrackImportHandler.class);

  private TrackExportFormat[] formats = { new ExtendedTrackFormat(), new ArtistTracknameFormat(), new CSVFormat() };

  private TrackService trackService;
  private TagManager tagService;
  private Playlist playlist;
  private int position;

  private List<TrackImportTask> tasks = new ArrayList<TrackImportTask>();

  public TrackImportHandler(TrackService titleService, TagManager titleTagService, Playlist playlist, int pos) {
    super();
    this.trackService = titleService;
    this.tagService = titleTagService;
    this.playlist = playlist;
    this.position = pos;
  }

  public boolean add(File file) throws IOException {
    log.info("add " + file);
    if (file.getAbsolutePath().toLowerCase().endsWith(".mp3")) {
      // can be used directly
      this.tasks.add(new MP3TrackImportTask(file));
      return true;
    } else if (file.getAbsolutePath().toLowerCase().endsWith(".m3u")) {
      this.addM3u(file, null);
      return true;
    } else if (file.getAbsolutePath().toLowerCase().endsWith(".m3u8")) {
      this.addM3u(file, "UTF-8");
      return true;
    } else {
      return this.addTextFile(file);
    }
  }

  public boolean add(String raw) {
    String[] lines = StringUtils.split(raw, "\n\r");
    TrackExportFormat format = this.detectFormat(lines);
    if (format != null) {
      for (String line : lines) {
        if (!line.startsWith("#")) {
          StringTrackImportTask importTask = new StringTrackImportTask(format, line);
          this.add(importTask);
        }
      }
      return true;
    } else {
      return false;
    }
  }

  public void clear() {
    this.tasks.clear();
  }

  private TrackExportFormat detectFormat(String[] lines) {
    // take the first 10 lines and return the format that accepts
    // most of them
    TrackExportFormat bestFormat = null;
    int maxAccepts = 0;

    int min = 0;
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].startsWith("#")) {
        min++;
      }
    }

    int max = Math.min(lines.length, min + 10);
    for (int f = 0; f < this.formats.length; f++) {
      int accepts = 0;
      for (int i = min; i < max; i++) {
        if (this.formats[f].supports(lines[i])) {
          accepts++;
        }
      }
      if (accepts == max) {
        return formats[f];
      } else {
        if (accepts > maxAccepts) {
          maxAccepts = accepts;
          bestFormat = this.formats[f];
        }
      }
    }

    return bestFormat;
  }

  public void add(TrackImportTask task) {
    this.tasks.add(task);
  }

  private boolean addTextFile(File file) throws IOException {
    String raw = FileUtils.readFileToString(file, "UTF-8");
    return this.add(raw);
  }

  private void addM3u(File file, String enc) throws IOException {
    File dir = file.getParentFile();
    String drive = "";
    if (dir.getAbsolutePath().charAt(1) == ':') {
      drive = dir.getAbsolutePath().substring(0, 2);
    }

    log.debug("reading m3u: " + file);
    Reader reader = null;
    if (enc != null) {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), enc), 1024 * 8);
    } else {
      reader = new BufferedReader(new FileReader(file), 1024 * 8);
    }
    StringBuffer text = new StringBuffer();
    char[] buffer = new char[1024 * 8];
    int chars = 0;
    do {
      chars = reader.read(buffer);
      if (chars > 0) {
        text.append(buffer, 0, chars);
      }
    } while (chars > 0);
    reader.close();

    String[] lines = StringUtils.split(text.toString(), "\n\r");
    log.debug(lines.length + " lines");

    for (String line : lines) {
      line = line.trim();
      if (line.length() > 1 && line.charAt(0) != '#') {
        String entryFilename = line;
        if (entryFilename.charAt(0) != '\\' && entryFilename.charAt(0) != '/' && entryFilename.charAt(1) != ':') {
          // entry is relative to m3u file
          entryFilename = dir.getAbsolutePath() + File.separatorChar + entryFilename;
        } else if (drive.length() > 0 && entryFilename.charAt(1) != ':') {
          entryFilename = drive + entryFilename;
        }
        File mp3File = new File(entryFilename);
        // System.out.println(mp3File);
        this.tasks.add(new MP3TrackImportTask(mp3File));
      }
    }
    log.debug(this.tasks.size() + " tasks");
  }

  private List<DetailedTrack> findBestMatches(List<DetailedTrack> candidates, TrackImportTask task) {
    ArrayList<DetailedTrack> best = new ArrayList<DetailedTrack>(candidates);

    int maxScore = 0;
    int[] scores = new int[best.size()];
    for (int i = 0; i < best.size(); i++) {
      // check for album
      if (StringUtils.equalsIgnoreCase(best.get(i).getAlbum(), task.getAlbum())) {
        scores[i] += 2;
      }
      // check if private track
      if (best.get(i).isPrivateTrack()) {
        scores[i] += 1;
      }
      maxScore = Math.max(scores[i], maxScore);
    }

    // remove all that don't have the max score
    if (maxScore > 0) {
      ArrayList<DetailedTrack> toBeRemoved = new ArrayList<DetailedTrack>();
      for (int i = 0; i < best.size(); i++) {
        if (scores[i] < maxScore) {
          toBeRemoved.add(best.get(i));
        }
      }
      best.removeAll(toBeRemoved);
    }

    return best;
  }

  /**
   * Gets the tasks
   * 
   * @return
   */
  public List<TrackImportTask> getTasks() {
    return tasks;
  }

  /**
   * Tries to resolve the ID3 tags for the tasks
   */
  public void resolveTags() {
    log.info("resolve tags");
    for (TrackImportTask task : tasks) {
      log.info("resolve tag for " + task.getSourceString());
      task.resolve();
    }
  }

  public boolean isEverythingResolved() {
    for (TrackImportTask task : tasks) {
      if (task.getStatus() != Status.RESOLVED) {
        return false;
      }
    }
    return true;
  }

  /**
   * Tries to find a track library title for the tasks within the local title registry
   */
  public void resolveTitlesLocal() {
    try {
      log.info("try to resolve titles within local title registry");
      TagSet currentSet = this.tagService.getCurrentTagSet();
      BitSet accepted = currentSet != null ? this.tagService.getTrackIds(currentSet) : null;
      for (TrackImportTask task : this.tasks) {
        if (task.getStatus() == Status.OPEN) {
          List<RegisteredTrack> titles = trackService.getTrackRegistry().search(task.getArtist(), task.getTitle());
          for (RegisteredTrack title : titles) {
            boolean accept = accepted == null || accepted.get(title.getId());
            if (accept) {
              task.setTrackLibraryTitle(title);
              task.setStatus(Status.RESOLVED);
              break;
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("error while resolving titles", e);
    }
  }

  /**
   * Sends search requests for the still unresolved tags to the server
   */
  public void resolveTitlesRemote() throws IOException, JSONException {
    log.info("try to resolve titles within online track library");
    TagSet currentSet = this.tagService.getCurrentTagSet();
    BitSet accepted = currentSet != null ? this.tagService.getTrackIds(currentSet) : null;
    for (TrackImportTask task : this.tasks) {
      if (task.getStatus() == Status.OPEN) {
        task.setStatus(Status.SEARCHING);
        List<DetailedTrack> titles = this.trackService.search(task.getArtist(), task.getTitle());
        if (accepted != null) {
          for (DetailedTrack title : new ArrayList<DetailedTrack>(titles)) {
            if (accepted.get(title.getId()) == false && this.trackService.getTrackRegistry().getTrack(title.getId()) != null) {
              // reject title
              titles.remove(title);
            }
          }
        }

        if (titles.size() > 0) {
          if (titles.size() > 1) {
            titles = this.findBestMatches(titles, task);
          }
          if (titles.size() == 1) {
            task.setTrackLibraryTitle(titles.get(0));
            task.setStatus(Status.RESOLVED);
          } else {
            task.setTrackLibraryTitle(titles.get(0));
            task.setCandidates(titles);
            task.setStatus(Status.MULTIPLE_CANDIDATES);
          }

        } else {
          task.setStatus(Status.NO_CANDIDATES);
        }
      }
    }
  }

  public Playlist getPlaylist() {
    return playlist;
  }

  public int getPosition() {
    return position;
  }

  /**
   * Adds the resolved titles of the tasks to the playlist
   */
  public void addTracksToPlaylist() {
    List<BasicTrack> titles = new ArrayList<BasicTrack>();
    for (TrackImportTask task : tasks) {
      if (task.getTrackLibraryTitle() != null) {
        titles.add(task.getTrackLibraryTitle());
      }
    }
    if (titles.size() == 1) {
      this.playlist.insertTrack(this.position, titles.get(0));
    } else if (titles.size() > 1) {
      this.playlist.insertTracks(this.position, titles);
    }
  }

}
