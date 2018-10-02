/**
 * 
 */
package de.stationadmin.gui.help;

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.Version;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;

/**
 * 
 * @author Frank Korf
 * 
 */
public class AboutDlg extends JDialog {
  private ClientContext ctx;

  public AboutDlg(ClientContext ctx) {
    this.ctx = ctx;
    this.init();
  }

  private void init() {
    this.setTitle(ctx.getString("about.title"));
    this.getContentPane().setLayout(new FormLayout("15dlu,pref:grow,15dlu", "15dlu,pref,10dlu,pref,3dlu,pref,5dlu,pref,8dlu:grow,pref,15dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;
    
    JLabel product = new JLabel("Station Admin " + Version.VERSION);
    Font font = product.getFont();
    product.setFont(new Font(font.getFamily(), Font.BOLD, font.getSize() + 4));
    this.getContentPane().add(product, cc.xy(2, row));
    row += 2;
    
    this.getContentPane().add(new JLabel(ctx.getString("about.license")), cc.xy(2, row));
    row += 2;
    this.getContentPane().add(new JLabel("(c) 2009 - 2018 Frank Korf"), cc.xy(2, row));
    row += 2;

    this.getContentPane().add(new JLabel(ctx.getString("about.apache")), cc.xy(2, row));
    row += 2;
    
    
    JButton okBtn = new JButton(new DisposeAction(this, ctx.getString("ok")));
    this.getContentPane().add(okBtn, cc.xy(2, row, CellConstraints.CENTER, CellConstraints.CENTER));
    
    
    this.setSize(400, 250);
    SwingTools.centerWithin(ctx.getRootWindow(), this);
  }

}
