/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.awt.GridLayout;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.JXErrorPane;

import sun.swing.SwingUtilities2;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;

/**
 * Displays the HTML representation of the schedule in a text area and allows to
 * copy the code to the clipboard
 * 
 * @author korf
 */
public class HTMLExportDlg extends JFrame {
  private ClientContext ctx;
  private ScheduleTableModel model;
  private ValueModel template = new ValueHolder("Tabelle einfach");
  private ValueModel html = new ValueHolder();

  /**
   * @param model
   */
  public HTMLExportDlg(ClientContext ctx, ScheduleTableModel model) {
    super();
    this.ctx = ctx;
    this.model = model;
    this.init();
    try {
      this.html.setValue(model.toHtml("Tabelle einfach"));
    } catch (Exception e) {
      JXErrorPane.showDialog(this, ctx.createErrorInfo(e, "schedulehtml.error.template"));
    }
  }

  private List<String> getTemplates() {
    ArrayList<String> list = new ArrayList<String>();
    try {
      // determine location of directory that contains the standard templates
      URL url = this.getClass().getClassLoader().getResource("Tabelle einfach.vm");
      File file = new File(url.toURI());
      File dir = file.getParentFile();
      // search for other templates in that directory
      if (dir.exists() && dir.isDirectory()) {
        File[] files = dir.listFiles();
        if (files != null) {
          for (File f : files) {
            if (f.getName().toLowerCase().endsWith(".vm")) {
              list.add(f.getName().substring(0, f.getName().length() - 3));
            }
          }
        }
        Collections.sort(list);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (list.size() == 0) {
      String[] options = { "Tabelle einfach", "Tabelle farbig" };
      return Arrays.asList(options);
    } else {
      return list;
    }
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,3dlu,pref:grow,8dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    JTextArea htmlTf = ctx.getComponentFactory().createTextArea(html);
    htmlTf.setRows(15);
    htmlTf.setColumns(40);
    this.getContentPane().add(new JScrollPane(htmlTf), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    SelectionInList<String> templateList = new SelectionInList<String>(getTemplates(), this.template);
    JComboBox templateCmb = BasicComponentFactory.createComboBox(templateList);
    this.getContentPane().add(templateCmb, cc.xy(2, 2));

    this.setSize(600, 500);
    this.setTitle(ctx.getString("schedulehtml.title"));
    SwingTools.centerOnScreen(this);

    this.template.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        try {
          html.setValue(model.toHtml((String) evt.getNewValue()));
        } catch (Exception e) {
          JXErrorPane.showDialog(HTMLExportDlg.this, ctx.createErrorInfo(e, "schedulehtml.error.template"));
        }
      }

    });

    JPanel buttonBar = new JPanel(new GridLayout(1, 3, 5, 5));

    if (SwingUtilities2.canAccessSystemClipboard()) {
      JButton clipboardBtn = new JButton(ctx.getString("schedulehtml.clipboard"));
      clipboardBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          StringSelection str = new StringSelection((String) html.getValue());
          HTMLExportDlg.this.getToolkit().getSystemClipboard().setContents(str, str);
        }

      });
      buttonBar.add(clipboardBtn);
    }

    JButton saveAction = new JButton(ctx.getString("save"));
    saveAction.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(HTMLExportDlg.this) == JFileChooser.APPROVE_OPTION) {
          try {
            FileUtils.writeStringToFile(fileChooser.getSelectedFile(), (String) html.getValue());
          } catch (IOException e) {
            JXErrorPane.showDialog(HTMLExportDlg.this, ctx.createErrorInfo(e, "schedulehtml.error.save"));
          }

        }
      }

    });
    buttonBar.add(saveAction);

    buttonBar.add(new JButton(new DisposeAction(this, ctx.getString("close"))));

    this.getContentPane().add(buttonBar, cc.xy(2, 6, CellConstraints.CENTER, CellConstraints.CENTER));

  }

}
