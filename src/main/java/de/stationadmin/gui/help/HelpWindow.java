/**
 * 
 */
package de.stationadmin.gui.help;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.SwingTools;

/**
 * 
 * @author Frank Korf
 * 
 */
public class HelpWindow extends JFrame {
  private ClientContext ctx;
  private JEditorPane htmlPane;

  public HelpWindow(ClientContext ctx) {
    this.ctx = ctx;
    this.init();
  }

  private void init() {
    this.htmlPane = new JEditorPane();
    this.htmlPane.setPreferredSize(new Dimension(50, 50));
    this.htmlPane.setEditable(false);
    this.getContentPane().setLayout(new FormLayout("10dlu,pref:grow,10dlu", "10dlu,pref:grow,10dlu"));
    CellConstraints cc = new CellConstraints();
    this.getContentPane().add(new JScrollPane(this.htmlPane), cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    this.setSize(400, 500);
    this.setTitle(ctx.getString("help.title"));
    SwingTools.centerOnScreen(this);

    this.show(this.getClass().getClassLoader().getResource("help.html"));
  }

  void show(URL url) {
    try {
      this.htmlPane.setPage(url);
    } catch (IOException e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "help.error.pageload"));
    }
  }

}
