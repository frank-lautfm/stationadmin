/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;

import javax.swing.JTabbedPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;

/**
 * @author korf
 *
 */
public class ScheduleWindow extends StationAdminFrame {
  private static final long serialVersionUID = -4605536430316908059L;

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
    tabPane.addTab(ctx.getString("scheduleeditor.tab.basis"), new ScheduleEditor(ctx));
    tabPane.addTab(ctx.getString("scheduleeditor.tab.event"), new ScheduleEventEditor(ctx));

    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(tabPane, BorderLayout.CENTER);

    Dimension dim = this.getPreferredSize();
    this.setSize(850, (int) dim.getHeight() + 80);
    this.setTitle(ctx.getString("scheduleeditor.title"));
  }

  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(850, 600);
  }

}
