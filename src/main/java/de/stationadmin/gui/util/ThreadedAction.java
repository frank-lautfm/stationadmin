/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.ClientContext;

/**
 * @author Frank Korf
 * 
 */
public abstract class ThreadedAction extends AbstractAction {
  private static final long serialVersionUID = 3801100500595927155L;

  private ActionRunner actionRunner;
  private JLabel statusLabel;
  private JRootPane rootFrame = AppUtils.getRootFrame().getRootPane();

  public ThreadedAction(ClientContext ctx) {
    this();
  }

  public ThreadedAction() {
    super();
  }

  public ThreadedAction(JRootPane rootPane) {
    super();
    if (rootPane != null) {
      this.rootFrame = rootPane;
    }
  }
  
  protected boolean beforeExecution() {
    return true;
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public final void actionPerformed(ActionEvent evt) {
    if(!beforeExecution()) {
      return;
    }

    JPanel glass = (JPanel) this.rootFrame.getGlassPane();
    glass.removeAll();
    glass.setLayout(new GridBagLayout());

    JPanel statusPanel = new JPanel(new FormLayout("5dlu,max(130dlu;pref),5dlu", "5dlu,max(10dlu;pref),5dlu"));

    this.statusLabel = new JLabel();
    this.statusLabel.setFont(new Font(ComponentFactory.boldLabelFont.getFamily(), 0, ComponentFactory.boldLabelFont
        .getSize() + 2));
    this.statusLabel.setBackground(Color.LIGHT_GRAY);
    statusPanel.add(statusLabel, new CellConstraints(2, 2, CellConstraints.CENTER, CellConstraints.CENTER));

    this.actionRunner = new ActionRunner();
    this.actionRunner.start();

    glass.add(statusPanel);
    glass.setVisible(true);

    Observer observer = new Observer();
    observer.start();
  }

  /**
   * Gets the current status of the action
   * 
   * @return status
   */
  protected abstract String getStatus();

  /**
   * Invoked after a successful execution of the action in EDT
   */
  protected void onSuccess() {

  }

  protected abstract void performAction() throws Exception;

  protected abstract void showError(Exception e);

  private class ActionRunner extends Thread {
    private volatile Exception exception;

    public Exception getException() {
      return exception;
    }

    @Override
    public void run() {
      try {
        performAction();
      } catch (Exception e) {
        this.exception = e;
      }
    }
  }

  private class Observer extends Thread {

    @Override
    public void run() {
      try {
        while (actionRunner.isAlive()) {
          final String status = getStatus();
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              statusLabel.setText(status);
            }
          });
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
          }
        }
      } finally {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            rootFrame.getGlassPane().setVisible(false);
            if (actionRunner.getException() == null) {
              onSuccess();
            }

          }
        });

      }
      if (actionRunner.getException() != null) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            showError(actionRunner.getException());
          }
        });
      }

    }

  }
}
