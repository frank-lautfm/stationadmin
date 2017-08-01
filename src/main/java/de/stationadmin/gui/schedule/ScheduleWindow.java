/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;

/**
 * @author korf
 *
 */
public class ScheduleWindow extends StationAdminFrame {
  private static final long serialVersionUID = -4605536430316908059L;

  private ScheduleTableModel model;

  /**
   * @param ctx
   * @throws HeadlessException
   */
  public ScheduleWindow(ClientContext ctx) throws HeadlessException {
    super(ctx, "schedule");
    this.init();
  }

  private void init() {
    JTabbedPane tabPane = new JTabbedPane(JTabbedPane.BOTTOM);
    ScheduleEditor editor = new ScheduleEditor(ctx);
    this.model = editor.getModel();
    tabPane.addTab(ctx.getString("scheduleeditor.tab.basis"), editor);
    tabPane.addTab(ctx.getString("scheduleeditor.tab.event"), new ScheduleEventEditor(ctx));

    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(tabPane, BorderLayout.CENTER);

    Dimension dim = this.getPreferredSize();
    this.setSize(850, (int) dim.getHeight() + 80);
    this.setTitle(ctx.getString("scheduleeditor.title"));

    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        boolean modified = (Boolean) model.getModified().getValue();
        if (!modified) {
          dispose();
        } else {
          int result = JOptionPane.showConfirmDialog(null, ctx.getString("scheduleeditor.msg.confirmdirty"), null, JOptionPane.YES_NO_OPTION);
          if (result == JOptionPane.YES_OPTION) {
            dispose();
          }
        }
      }

    });

  }

  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(850, 600);
  }

}
