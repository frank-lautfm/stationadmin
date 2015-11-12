/**
 * 
 */
package de.stationadmin.base.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.mp3splitter.MP3Splitter;
import de.stationadmin.base.mp3splitter.SplitPoint;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.TrackMatcher;
import de.stationadmin.lfm.backend.ProgressListener;

/**
 * Toll class for uploading DJ mixes
 * 
 * @author korf
 */
public class MixUploader {
  private static class IdMatcher implements TrackMatcher {
    private int id;

    IdMatcher(int id) {
      this.id = id;
    }

    /**
     * @see de.stationadmin.base.track.TrackMatcher#matches(de.emjoy.stationadmin.raw.SearchResult)
     */
    @Override
    public boolean matches(DetailedTrack result) {
      return result.getId() == id;
    }
  }

  protected static final Logger log = Logger.getLogger(MixUploader.class);
  private StationAdminClient client;
  private String sourceFile;
  private String targetPlaylist;
  private String tag;
  private String trackDir;

  private List<SplitPoint> splitPoints = new ArrayList<SplitPoint>();

  private List<File> tracks;

  protected MixUploader(StationAdminClient client) {
    this.client = client;
  }

  public MixUploader(StationAdminClient client, String sourceFile, String targetPlaylist) {
    super();
    this.client = client;
    this.sourceFile = sourceFile;
    this.targetPlaylist = targetPlaylist;
  }

  public void addSplitPoint(SplitPoint sp) {
    this.splitPoints.add(sp);
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public void split() throws Exception {
    MP3Splitter splitter = new MP3Splitter();
    File file = new File(this.sourceFile);
    this.tracks = splitter.split(file, splitPoints, this.trackDir == null ? file.getParentFile() : new File(this.trackDir));
    log.info("splitted into " + this.tracks.size() + " tracks");
  }

  public void upload() throws Exception {
    if (this.tracks != null && this.tracks.size() > 0) {
      Playlist playlist = null;
      StringBuffer available = new StringBuffer();
      for (Playlist pl : this.client.getPlaylistService().getPlaylistRegistry().getAllPlaylists()) {
        available.append(pl.getName() + "\n");
        if (pl.getName().equalsIgnoreCase(this.targetPlaylist)) {
          playlist = pl;
        }
      }
      if (playlist == null) {
        log.info(this.targetPlaylist + " not found - available\n" + available.toString());
        throw new Exception("playlist not found");
      }
      playlist.removeEntries(new ArrayList<Entry>(playlist.getEntries()));

      for (File track : this.tracks) {
        log.info("upload " + track.getName() + " / " + (track.length() / 1024) + " kb");
        this.client.getTrackService().upload(track, new ProgressListener() {

          @Override
          public void setMaxValue(int max) {
          }

          @Override
          public void setCurrentValue(int value) {
          }

          @Override
          public boolean isAbortCurrent() {
            return false;
          }
        });
      }
//      HashMap<String, UploadedTitle> titlesByFilename = new HashMap<String, UploadedTitle>();
//      List<UploadedTitle> titles = this.client.getTitleService().getUnconfirmedUploadedTitles();
//      for (UploadedTitle title : titles) {
//        title.setPrivateTrack(true);
//        title.setResume(true);
//        titlesByFilename.put(title.getFilenameRaw(), title);
//        log.info("found " + title.getId() + " / " + title.getArtist() + " / " + title.getTitle() + " / " + title.getFilenameRaw());
//      }
//      log.info("save " + titles.size() + " titles");
//      this.client.getTitleService().confirmUploadedTitles(titles);
//      Thread.sleep(1000);
//      this.client.getTitleService().synchronize();
//
//      for (File track : this.tracks) {
//        UploadedTitle title = titlesByFilename.get(track.getName());
//        if (title != null) {
//          Title t = this.client.getTitleService().getTitleRegistry().getTitle(title.getId());
//          if (t == null) {
//            TitleQuery query = new TitleQuery();
//            query.setTitle(title.getTitle());
//            List<DetailedTitle> result = this.client.getTitleService().findAll(query, new IdMatcher(title.getId()), true);
//            t = result.size() > 0 ? result.get(0) : null;
//          }
//          if (t != null) {
//            playlist.addTitle(t);
//            if (this.tag != null) {
//              this.client.getTitleTagManager().tagTitles(this.tag, t.getId());
//            }
//          } else {
//            log.info("title not found: " + title.getArtist() + " - " + title.getTitle());
//          }
//        } else {
//          log.warn("missing title: " + track.getName());
//        }
//      }
//
//      this.client.getPlaylistService().savePlaylist(playlist);

    }
  }

  public String getSourceFile() {
    return sourceFile;
  }

  public void setSourceFile(String sourceFile) {
    this.sourceFile = sourceFile;
  }

  public String getTargetPlaylist() {
    return targetPlaylist;
  }

  public void setTargetPlaylist(String targetPlaylist) {
    this.targetPlaylist = targetPlaylist;
  }

  public String getTrackDir() {
    return trackDir;
  }

  public void setTrackDir(String trackDir) {
    this.trackDir = trackDir;
  }

}
