/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.base.tag.TagManager;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tag.TagNameDlg;

/**
 * Adds or removes a tag from / to selected titles
 * 
 * @author Frank Korf
 */
public class TrackTagAction extends AbstractAction {
  private static final long serialVersionUID = 7586385362164090723L;
  private TagManager tagManager;
  private String tagName;
  private int[] titleIds;
  private boolean tag;
  private TextProvider textProvider;

  /**
   * Constructor
   * @param tagManager tag manager
   * @param tagName name of the tag
   * @param tag <code>true</code> to add tag, <code>false</code> to remove tag
   */
  public TrackTagAction(TagManager tagManager, TextProvider textProvider, String tagName, boolean tag) {
    super();
    this.tagManager = tagManager;
    this.tagName = tagName;
    this.tag = tag;
    this.textProvider = textProvider;
    if (tagName != null) {
      this.putValue(Action.NAME, tagName);
    } else {
      this.putValue(Action.NAME, "Neues Tag");
    }
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    if (this.titleIds != null) {
      String tagName = this.tagName;
      if (tagName == null) {
        TagNameDlg dlg = new TagNameDlg(textProvider);
        dlg.setModal(true);
        dlg.setVisible(true);
        if (dlg.isAccepted() && dlg.getTagName() != null) {
          tagName = dlg.getTagName();
        }
      }
      if (tagName != null) {
        try {
          if (tag) {
            this.tagManager.tagTracks(tagName, titleIds);
          } else {
            this.tagManager.untagTracks(tagName, titleIds);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * @return the titleIds
   */
  public int[] getTitleIds() {
    return titleIds;
  }

  /**
   * @param titleIds
   *          the titleIds to set
   */
  public void setTitleIds(int[] titleIds) {
    this.titleIds = titleIds;
  }


}
