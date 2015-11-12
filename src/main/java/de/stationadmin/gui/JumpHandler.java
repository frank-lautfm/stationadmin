/**
 * 
 */
package de.stationadmin.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Frank
 *
 */
public class JumpHandler {
  private List<JumpListener> jumpListeners = new ArrayList<JumpListener>();

  /**
   * Registers a jump listener
   * @param listener
   */
  public void addJumpListener(JumpListener listener) {
    this.jumpListeners.add(listener);
  }

  /**
   * Invokes a {@link JumpListener#jumpTo(Object)} on all registered listeners
   * @param target
   */
  public void jumpTo(Object target) {
    for (JumpListener listener : this.jumpListeners) {
      listener.jumpTo(target);
    }
  }

}
