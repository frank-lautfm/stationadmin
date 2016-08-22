package de.stationadmin.gui.upload;

import java.awt.Component;
import java.io.File;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;

import de.stationadmin.base.track.upload.QueuedTrack;
import de.stationadmin.gui.util.AppUtils;

/**
 * @author Frank
 *
 */
public class QueuedTrackListCellRenderer extends DefaultListCellRenderer {
  private static final long serialVersionUID = 7114231066117583356L;

  private ImageIcon privateTrack;
  private ImageIcon publicTrack;
  
  
  public QueuedTrackListCellRenderer() {
    this.privateTrack = AppUtils.getIcon("trackprivate.png");
    this.publicTrack = AppUtils.getIcon("trackpublic.png");
  }
  
  /**
   * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
   */
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    QueuedTrack track = (QueuedTrack)value;
    setIcon(track.getFile().isPrivateTrack() ? privateTrack : publicTrack);
    setText(track.getFile().getFile().getName());
    return this;
  }
}