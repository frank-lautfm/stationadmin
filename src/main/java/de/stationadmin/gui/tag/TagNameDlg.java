package de.stationadmin.gui.tag;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;

public class TagNameDlg extends JDialog {
  private static final long serialVersionUID = 5114286443127515735L;
  private boolean accepted = false;
  private TextProvider textProvider;
  private JTextField tf;

  public TagNameDlg(TextProvider txtProvider) {
    this.setTitle("Tag");
    this.textProvider = txtProvider;
    tf = new JTextField(20);
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref,5dlu"));
    this.getContentPane().add(tf, new CellConstraints(2, 2));
    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));

    JButton okBtn = new JButton("Ok");
    okBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        String name = tf.getText();
        if (name.length() == 0) {
          ErrorInfo errorInfo = textProvider.createErrorInfo(null, "titletagmanager.action.save.illegalname.empty");
          JXErrorPane.showDialog(null, errorInfo);
        } else if (name.contains("/")) {
          ErrorInfo errorInfo = textProvider.createErrorInfo(null, "titletagmanager.action.save.illegalname.slash");
          JXErrorPane.showDialog(null, errorInfo);
        } else {

          accepted = true;
          dispose();
        }
      }

    });

    buttonPanel.add(okBtn);
    buttonPanel.add(new JButton(new DisposeAction(this, "Abbruch"))); // FIXME
    // localize
    this.getContentPane().add(buttonPanel, new CellConstraints(2, 4, CellConstraints.CENTER, CellConstraints.CENTER));

    this.pack();
    SwingTools.centerOnScreen(this);
  }

  public void setTagName(String name) {
    this.tf.setText(name);
  }

  /**
   * @return the accepted
   */
  public boolean isAccepted() {
    return accepted;
  }

  public String getTagName() {
    return StringUtils.trimToNull(this.tf.getText());
  }
}
