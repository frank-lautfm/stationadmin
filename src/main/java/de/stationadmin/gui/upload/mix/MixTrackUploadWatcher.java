/**
 * 
 */
package de.stationadmin.gui.upload.mix;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.upload.QueuedTrack;
import de.stationadmin.base.track.upload.UploadManager;

/**
 * @author korf
 *
 */
public class MixTrackUploadWatcher implements PropertyChangeListener {
  private TagManager tagManager;
  private UploadManager uploadManager;
  private List<File> files;
  private Playlist targetPlaylist;
  private String tag;
  Map<File, DetailedTrack> tracksByFiles = new HashMap<File, DetailedTrack>();

  /**
   * @param uploadManager
   * @param files
   * @param targetPlaylist
   * @param tag
   */
  public MixTrackUploadWatcher(UploadManager uploadManager, TagManager tagManager, List<File> files, Playlist targetPlaylist, String tag) {
    super();
    this.uploadManager = uploadManager;
    this.tagManager = tagManager;
    this.files = files;
    this.targetPlaylist = targetPlaylist;
    this.tag = tag;

  }
  
  public void startWatching() {
    this.uploadManager.addPropertyChangeListener("trackCompleted", this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    this.checkTracks();
  }

  public void checkTracks() {
    for (QueuedTrack track : uploadManager.getProcessedTracks()) {
      tracksByFiles.put(track.getFile().getFile(), track.getTrack());
    }
    if (tracksByFiles.size() >= files.size()) {
      List<DetailedTrack> tracks = new ArrayList<DetailedTrack>();
      int[] ids = new int[files.size()];
      for (int i = 0; i < files.size(); i++) {
        DetailedTrack track = tracksByFiles.get(files.get(i));
        if (track != null) {
          tracks.add(track);
          ids[i] = track.getId();
        } else {
          // still files missing
          return;
        }
      }

      if (tracks.size() == files.size()) {
        // all tracks found
        if (tag != null) {
          try {
            tagManager.tagTracks(tag, ids);
          } catch (Exception e) {
          }
        }
        if(targetPlaylist != null) {
          for(DetailedTrack track : tracks) {
            targetPlaylist.addTrack(track);
          }
        }
        
        uploadManager.removePropertyChangeListener("trackCompleted", this);
      }

    }

  }

}
