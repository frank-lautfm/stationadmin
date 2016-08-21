/**
 * 
 */
package de.stationadmin.gui.live;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jgoodies.binding.value.ValueModel;

public class FileSelectionAction extends AbstractAction {
  private static final long serialVersionUID = -2063331600015348910L;
  private ValueModel model;
  private ValueModel lastDir;
  private String extension;

  public FileSelectionAction(ValueModel model, ValueModel lastDir, String extension) {
    this.putValue(Action.NAME, "...");
    this.model = model;
    this.lastDir = lastDir;
    this.extension = extension;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    JFileChooser fileChooser = new JFileChooser();
    String value = (String) model.getValue();
    if (value != null && value.length() > 0) {
      File file = new File(value);
      fileChooser.setCurrentDirectory(file.getParentFile());
      fileChooser.setSelectedFile(file);
    } else if (lastDir.getValue() != null) {
      fileChooser.setCurrentDirectory(new File((String)lastDir.getValue()));
    }
    fileChooser.setFileFilter(new FileNameExtensionFilter(this.extension, extension));
    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      if (!fileChooser.getSelectedFile().isDirectory()) {
        lastDir.setValue(fileChooser.getSelectedFile().getAbsolutePath());
        model.setValue(fileChooser.getSelectedFile().getAbsolutePath());
      }
    }

  }

}