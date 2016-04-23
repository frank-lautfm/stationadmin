package de.stationadmin.gui.playlist;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.exporter.PlaylistExporter;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AbstractFileDialogAction;
import de.stationadmin.gui.util.AppUtils;

public class PlaylistExportAction extends AbstractFileDialogAction {
  private static final long serialVersionUID = -7107367385022205566L;
  private TextProvider textProvider;
  private ValueModel playlistHolder;
  private PlaylistExporter exporter;

  public PlaylistExportAction(ClientContext ctx, ValueModel playlistHolder, String format, PlaylistExporter exporter) {
    super();
    this.textProvider = ctx.getTextProvider();
    this.playlistHolder = playlistHolder;
    this.exporter = exporter;
    this.setOpenDialog(false);
    this.setUserPrefencesKey("filedir.playlistexport");
    FileFilter filter = new FileNameExtensionFilter(this.textProvider.getString("action.playlistexport." + format + ".name"), format);
    this.setFileFilter(filter);
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.playlistexport." + format + ".name"));
  }

  protected void beforeDisplay(JFileChooser chooser) {
    Playlist playlist = (Playlist) this.playlistHolder.getValue();
    if (playlist.getName() != null) {
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory().getAbsolutePath() + File.separatorChar + playlist.getName()));
    }
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    Playlist playlist = (Playlist) this.playlistHolder.getValue();
    if (playlist != null) {
      super.actionPerformed(evt);
    }
  }

  @Override
  protected void performAction(JFileChooser fileChooser, File file) {
    Playlist playlist = (Playlist) this.playlistHolder.getValue();
    String name = file.getAbsolutePath();
    int pos = file.getName().lastIndexOf('.');
    String extension = pos > 0 ? file.getName().substring(pos + 1) : null;

    if (extension == null) {
      extension = ((FileNameExtensionFilter) fileChooser.getFileFilter()).getExtensions()[0];
      name = name + "." + extension;
    }
    File exportFile = new File(name);
    if (!exportFile.exists()
        || JOptionPane.showConfirmDialog(AppUtils.getRootFrame(), textProvider.getString("action.playlistexport.confirm.msg"), textProvider.getString("action.playlistexport.confirm.title"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
      try {
        exporter.toFile(playlist, exportFile);
      } catch (IOException e) {
        JXErrorPane.showDialog(null, this.textProvider.createErrorInfo(e, "action.playlistexport.error"));
      }
    }
  }

}
