/**
 * 
 */
package de.stationadmin.gui.subscriptions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.gui.ClientContext;

/**
 * @author korf
 *
 */
@SuppressWarnings("unchecked")
public class SubscriptionResultDeleteAction extends AbstractAction {
  private static final long serialVersionUID = -8417438026067373518L;
  
  private ClientContext ctx;
  private ValueModel selectionHolder;
  
  public SubscriptionResultDeleteAction(ClientContext ctx, ValueModel  selectionHolder) {
    super();
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.subscriptionResult.delete"));
    this.ctx = ctx;
    this.selectionHolder = selectionHolder;
    this.selectionHolder.addValueChangeListener(new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(evt.getNewValue() != null && ((List<DetailedTrack>)evt.getNewValue()).size() > 0);
        
      }
    });
    setEnabled(false);
  }


  

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if(selectionHolder.getValue() != null) {
      ctx.getAdminClient().getSubscriptionService().remove((List<DetailedTrack>)this.selectionHolder.getValue());
    }

  }

}
