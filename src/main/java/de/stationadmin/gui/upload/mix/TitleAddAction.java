/**
 * 
 */
package de.stationadmin.gui.upload.mix;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.TrackMatcher;
import de.stationadmin.base.track.TrackQuery;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ThreadedAction;

/**
 * @author Frank
 * 
 */
public class TitleAddAction extends ThreadedAction {
  private static final long serialVersionUID = 7869707187604661052L;

  private TrackService titleService;
  private TextProvider textProvider;
  private List<File> files;
  private Map<File, DetailedTrack> uploadedTitleMap;
  private Playlist targetPlaylist;
  private Map<File, DetailedTrack> detailedTitleMap = new HashMap<File, DetailedTrack>();
  private volatile File currentFile;

  /**
   * @param ctx
   */
  public TitleAddAction(ClientContext ctx, List<File> files, Map<File, DetailedTrack> titleMap, Playlist targetPlaylist) {
    super(ctx);
    this.titleService = ctx.getAdminClient().getTrackService();
    this.textProvider = ctx.getTextProvider();
    this.files = files;
    this.uploadedTitleMap = titleMap;
    this.targetPlaylist = targetPlaylist;
  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#getStatus()
   */
  @Override
  protected String getStatus() {
    return this.textProvider.getString("upload.mix.action.titleadd.status", this.currentFile != null ? this.currentFile.getName() : "");
  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#performAction()
   */
  @Override
  protected void performAction() throws Exception {
    for (File file : files) {
      this.currentFile = file;
      DetailedTrack title = uploadedTitleMap.get(file);
      if (title != null) {
        TrackQuery query = new TrackQuery();
        query.setTitle(title.getTitle());
        List<DetailedTrack> t = titleService.findAll(query, new IdMatcher(title.getId()), true);
        if (t.size() > 0) {
          detailedTitleMap.put(file, t.get(0));
        }
      }
    }

  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#showError(java.lang.Exception)
   */
  @Override
  protected void showError(Exception e) {
    ErrorInfo errorInfo = this.textProvider.createErrorInfo(e, "upload.mix.action.titleadd.failed");
    JXErrorPane.showDialog(AppUtils.getRootFrame(), errorInfo);
  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#onSuccess()
   */
  @Override
  protected void onSuccess() {
    List<File> missing = new ArrayList<File>();
    for (File file : files) {
      DetailedTrack title = detailedTitleMap.get(file);
      if (title != null) {
        targetPlaylist.addTrack(title);
      } else {
        missing.add(file);
      }
    }
    if (missing.size() > 0) {
      JOptionPane.showMessageDialog(AppUtils.getRootFrame(),
          textProvider.getString("upload.mix.action.titleadd.missing.msg", Integer.toString(missing.size()), Integer.toString(files.size())),
          textProvider.getString("upload.mix.action.titleadd.missing.title"), JOptionPane.WARNING_MESSAGE);
    }
  }

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

}
