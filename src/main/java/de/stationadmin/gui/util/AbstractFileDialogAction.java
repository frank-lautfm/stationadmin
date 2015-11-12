/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.jgoodies.binding.value.ValueHolder;

/**
 * @author korf
 * 
 */
public abstract class AbstractFileDialogAction extends AbstractAction {
  private static final long serialVersionUID = 3561855938918905424L;
  private ValueHolder directoryHolder = new ValueHolder();
  private boolean openDialog = true;
  private String title;
  private FileFilter fileFilter;
  private String userPrefencesKey;
  private Component parent = AppUtils.getRootFrame();
  
  /**
   * @see
   * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle(this.title);
    if(this.fileFilter != null) {
      chooser.setFileFilter(this.fileFilter);
    }
    File lastDir = (File)this.directoryHolder.getValue();
    if(lastDir == null && this.userPrefencesKey != null) {
      String last = Preferences.userRoot().get(this.userPrefencesKey, null);
      if(last != null) {
        lastDir = new File(last);
      }
    }
    if(lastDir != null && lastDir.exists()) {
      chooser.setCurrentDirectory(lastDir);
    }
    this.beforeDisplay(chooser);
    if(this.openDialog) {
      if(chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
        this.proceedWithAcceptedFile(chooser, chooser.getSelectedFile());
      }
    }
    else {
      if(chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
        this.proceedWithAcceptedFile(chooser, chooser.getSelectedFile());
      }
    }

  }
  
  protected void beforeDisplay(JFileChooser chooser) {
    
  }
  
  private void proceedWithAcceptedFile(JFileChooser chooser, File file) {
    File dir = file.isDirectory() ? file : file.getParentFile();
    this.directoryHolder.setValue(dir);
    if(this.userPrefencesKey != null) {
      Preferences.userRoot().put(this.userPrefencesKey, dir.getPath());
    }
    this.performAction(chooser, file);
  }
  
  protected abstract void performAction(JFileChooser fileChooser, File file);

  public ValueHolder getDirectoryHolder() {
    return directoryHolder;
  }

  public void setDirectoryHolder(ValueHolder directoryHolder) {
    this.directoryHolder = directoryHolder;
  }

  public boolean isOpenDialog() {
    return openDialog;
  }

  public void setOpenDialog(boolean openDialog) {
    this.openDialog = openDialog;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public FileFilter getFileFilter() {
    return fileFilter;
  }

  public void setFileFilter(FileFilter fileFilter) {
    this.fileFilter = fileFilter;
  }

  public String getUserPrefencesKey() {
    return userPrefencesKey;
  }

  public void setUserPrefencesKey(String userPrefencesKey) {
    this.userPrefencesKey = userPrefencesKey;
  }

  public Component getParent() {
    return parent;
  }

  public void setParent(Component parent) {
    this.parent = parent;
  }

}
