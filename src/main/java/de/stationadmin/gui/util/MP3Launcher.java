/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;

/**
 * Tool class for launching an mp3 player. 
 * <p>
 * If a player is configured the configured player will be used. Otherwise it is
 * tried to open a default player via {@link Desktop#open(File)}.
 * 
 * @author Frank
 */
public class MP3Launcher {
  // private ClientContext ctx;
  private TextProvider textprovider;
  private String mp3Player;
  private Desktop desktop;

  /**
   * @param ctx
   */
  public MP3Launcher(ClientContext ctx) {
    super();
    this.textprovider = ctx.getTextProvider();
    this.mp3Player = ctx.getAdminClient().getSettings().getMp3Player();
    ctx.getAdminClient().getSettings().addPropertyChangeListener("mp3Player", new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        mp3Player = (String)evt.getNewValue();
      }
    });
    this.desktop = AppUtils.getDesktop();
  }
  
  public MP3Launcher() {
    this.textprovider = new TextProvider();
    this.desktop = Desktop.getDesktop();
  }


  public boolean isAvailable() {
    return mp3Player != null
        || (desktop != null && desktop.isSupported(Action.OPEN));
  }

  /**
   * Opens an mp3 player for the given file
   * @param file mp3 or m3u file
   */
  public void play(File file) {
    this.openFile(file);
  }

  /**
   * Writes the given files or URLs to a temporary playlist and tries to open this
   * playlist
   * @param filesOrUrls
   */
  public void play(String[] filesOrUrls) {
    StringBuffer buf = new StringBuffer();
    for (String file : filesOrUrls) {
      buf.append(file);
      buf.append('\n');
    }

    try {
      File file = File.createTempFile("mp3", ".m3u");
      file.deleteOnExit();
      FileWriter writer = new FileWriter(file);
      writer.write(buf.toString());
      writer.close();
      
      this.openFile(file);
    } catch (IOException e) {
      JXErrorPane.showDialog(null, textprovider.createErrorInfo(e, "mp3.createTmpM3u.failed"));
    }

  }

  protected void openFile(File file) {
    String player = mp3Player;
    if (player != null) {
      try {
        String[] cmd = {player, file.getAbsolutePath() };
        Runtime.getRuntime().exec(cmd);
      } catch (Exception e) {
        JXErrorPane.showDialog(null, textprovider.createErrorInfo(e, "mp3.playWithCustom.failed", player));
      }
    } else {
      try {
        desktop.open(file);
      } catch (Exception e) {
        JXErrorPane.showDialog(null, textprovider.createErrorInfo(e, "mp3.playWithDefault.failed"));
      }
    }
  }

}
