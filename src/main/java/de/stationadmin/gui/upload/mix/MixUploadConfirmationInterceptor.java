/**
 * 
 */
package de.stationadmin.gui.upload.mix;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.upload.TitleConfirmationInterceptor;

/**
 * @author Frank
 */
public class MixUploadConfirmationInterceptor implements TitleConfirmationInterceptor {
  private boolean active = true;
  private ClientContext ctx;
  private List<File> files;
  private Playlist targetPlaylist;
  private String tag;
  private Map<String, File> filenameMap = new HashMap<String, File>();

  /**
   * @param files
   * @param targetPlaylist
   */
  public MixUploadConfirmationInterceptor(ClientContext ctx, List<File> files, Playlist targetPlaylist, String tag) {
    super();
    this.ctx = ctx;
    this.files = files;
    this.tag = tag;
    this.targetPlaylist = targetPlaylist;
    for (File file : files) {
      filenameMap.put(file.getName(), file);
    }
  }

  /**
   * @see de.stationadmin.gui.upload.TitleConfirmationInterceptor#beforeDisplay(java.util.List)
   */
  @Override
  public void beforeDisplay(List<DetailedTrack> titles) {
    if (active) {
//      for (UploadedTitle title : titles) {
//        if (filenameMap.containsKey(title.getFilenameRaw())) {
//          title.setPrivateTrack(true);
//          title.setResume(true);
//          title.setGenre(null);
//        }
//      }
    }
  }

  /**
   * @see de.stationadmin.gui.upload.TitleConfirmationInterceptor#beforeSave(java.util.List)
   */
  @Override
  public void beforeSave(List<DetailedTrack> titles) {
  }

  /**
   * @see de.stationadmin.gui.upload.TitleConfirmationInterceptor#afterSave(java.util.List)
   */
  @Override
  public void afterSave(List<DetailedTrack> titles) {
    if (active) {
//      if (this.targetPlaylist != null) {
//        try {
//          Map<File, DetailedTitle> map = new HashMap<File, DetailedTitle>();
//          int[] ids = new int[titles.size()];
//          int idIdx = 0;
//          for (DetailedTitle title : titles) {
//            if (filenameMap.containsKey(title.getFilenameRaw())) {
//              map.put(filenameMap.get(title.getFilenameRaw()), title);
//            }
//            ids[idIdx++] = title.getId();
//          }
//          if(this.tag != null) {
//            this.ctx.getAdminClient().getTitleTagManager().tagTitles(tag, ids);
//          }
//          if (map.size() > 1) {
//            TitleAddAction action = new TitleAddAction(ctx, files, map, targetPlaylist);
//            action.actionPerformed(new ActionEvent(this, 1, "add"));
//          }
//
//        } catch (Exception e) {
//          Logger.getLogger(MixUploadConfirmationInterceptor.class).error("unable to assign titles to playlist", e);
//        }
//      }
//      active = false;
    }
  }

}
