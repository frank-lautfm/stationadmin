package de.stationadmin.gui.help;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.Action;

public class HomepageAction extends AbstractAction {
  private static final long serialVersionUID = -2908748032381608591L;

  public HomepageAction() {
    this.putValue(Action.NAME, "Homepage");
    this.setEnabled(Desktop.isDesktopSupported());
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
    if (desktop != null) {
      try {
        desktop.browse(new URI("http://stationadmin.sourceforge.net/"));
      } catch (Exception e) {

      }
    }

  }

}
