/**
 * 
 */
package de.stationadmin.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.TimerTask;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * @author Frank
 *
 */
public class StationAdminFrame extends JFrame {
  private static final long serialVersionUID = -6620750221420097655L;
  protected ClientContext ctx;
  private String name = null;
  private boolean windowSizePositionChanged = false;
  
  /**
   * @param ctx
   * @throws HeadlessException
   */
  public StationAdminFrame(ClientContext ctx) throws HeadlessException {
    this(ctx, null);
  }
  
  public StationAdminFrame(ClientContext ctx, String name) throws HeadlessException {
    super();
    this.ctx = ctx;
    this.name = name;
    this.initFrame();
  }


  public StationAdminFrame(ClientContext ctx, String name, JComponent content) throws HeadlessException {
    super();
    this.ctx = ctx;
    this.name = name;
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(content, BorderLayout.CENTER);
    this.setTitle(ctx.getTextProvider().getString("tab." + name));
    this.initFrame();
  }
  
  protected Dimension getDefaultSize() {
    return new Dimension(900, 712);
  }
  
  private void initFrame() {
    String prefix = this.name != null ? this.name + ".window." : "window.";
    Dimension defaultDim = getDefaultSize();
    this.setSize(Preferences.userRoot().getInt(prefix + "w", defaultDim.width), Preferences.userRoot().getInt(prefix + "h", defaultDim.height));
    this.setLocation(Preferences.userRoot().getInt(prefix + "x", 10), Preferences.userRoot().getInt(prefix + "y", 10));

    this.addComponentListener(new ComponentAdapter() {

      @Override
      public void componentResized(ComponentEvent e) {
        windowSizePositionChanged = true;
      }

      @Override
      public void componentMoved(ComponentEvent e) {
        windowSizePositionChanged = true;
      }

    });
    this.ctx.getAdminClient().getSessionCtx().getTimer().schedule(new WindowSizePositionPersister(), 5000, 5000);
    
    this.setIconImage(Toolkit.getDefaultToolkit().getImage(
        this.getClass().getClassLoader().getResource("icons/trayicon.png")));

  }

  private class WindowSizePositionPersister extends TimerTask {

    @Override
    public void run() {
      if (windowSizePositionChanged) {
        String prefix = name != null ? name + ".window." : "window.";
        Preferences.userRoot().putInt(prefix + "x", getX());
        Preferences.userRoot().putInt(prefix + "y", getY());
        Preferences.userRoot().putInt(prefix + "w", getWidth());
        Preferences.userRoot().putInt(prefix + "h", getHeight());
        windowSizePositionChanged = false;
      }

    }

  }

}
