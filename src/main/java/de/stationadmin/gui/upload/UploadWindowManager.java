/**
 * 
 */
package de.stationadmin.gui.upload;

import de.stationadmin.gui.ClientContext;

/**
 *
 * @author Frank Korf
 *
 */
public class UploadWindowManager {
  private ClientContext ctx;
  private UploadWindow uploadWindow;

  public UploadWindowManager(ClientContext ctx) {
    super();
    this.ctx = ctx;
  }

  public UploadWindow getUploadWindow() {
    if(this.uploadWindow == null) {
      this.uploadWindow = new UploadWindow(ctx);
    }
    return this.uploadWindow;
  }
  
}
