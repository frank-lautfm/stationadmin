/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;

/**
 * Wrapper for Clipboard-based actions
 * 
 * @author Frank Korf
 */
public class ClipboardAction extends AbstractAction {
  private static final long serialVersionUID = -7480141516949042524L;
  private Action action;
  private JComponent component;

  public ClipboardAction(ClientContext ctx, JComponent component, ValueModel selectionHolder, Action action) {
    super();
    this.action = action;
    this.component = component;
    this.putValue(Action.NAME, ctx.getString("action.clipboard." + action.getValue(Action.NAME)));
    if (!action.getValue(Action.NAME).equals("paste")) {
      this.setEnabled(false);
      selectionHolder.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          boolean enabled = true;
          if (evt.getNewValue() == null || (evt.getNewValue() instanceof List && ((List<?>) evt.getNewValue()).size() == 0)) {
            enabled = false;
          }
          setEnabled(enabled);
        }

      });
    }
  }
  
  public ClipboardAction(TextProvider textProvider, JTextComponent component, Action action) {
    super();
    this.action = action;
    this.component = component;
    this.putValue(Action.NAME, textProvider.getString("action.clipboard." + action.getValue(Action.NAME)));
    if (!action.getValue(Action.NAME).equals("paste")) {
      this.setEnabled(component.getCaret().getDot() != component.getCaret().getMark());
      component.getCaret().addChangeListener(new ChangeListener() {
        
        @Override
        public void stateChanged(ChangeEvent e) {
          Caret caret = (Caret)e.getSource();
          setEnabled(caret.getDot() != caret.getMark());
        }
      });
    }
  }


  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    action.actionPerformed(new ActionEvent(component, e.getID(), e.getActionCommand()));
  }

}
