/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;

/**
 * @author korf
 * 
 */
public class SwingTools {

  public static void centerOnScreen(Component comp) {
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

    // Determine the new location of the window
    int w = comp.getSize().width;
    int h = comp.getSize().height;
    int x = (dim.width - w) / 2;
    int y = (dim.height - h) / 2;

    // Move the window
    comp.setLocation(x, y);

  }

  public static void centerWithin(JFrame frame, Component comp) {
    Point parentLocation = frame.getLocation();
    Dimension dim = frame.getSize();

    int w = comp.getSize().width;
    int h = comp.getSize().height;

    int x = (dim.width - w) / 2 + (int) parentLocation.getX();
    int y = (dim.height - h) / 2 + (int) parentLocation.getY();

    comp.setLocation(x, y);
  }
  
  public static void expandAllTreeNodes(JTree tree, int startingIndex, int rowCount){
    for(int i=startingIndex;i<rowCount;++i){
        tree.expandRow(i);
    }

    if(tree.getRowCount()!=rowCount){
      expandAllTreeNodes(tree, rowCount, tree.getRowCount());
    }
}
  
  public static void bindPopup(JComponent component, JPopupMenu popup) {
		component.getInputMap().put( KeyStroke.getKeyStroke(KeyEvent.VK_F10, KeyEvent.SHIFT_MASK), "popup");
		component.getInputMap().put( KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0), "popup");
		component.getActionMap().put("popup", new AbstractAction() {
			private static final long serialVersionUID = 4953175811731710767L;

			@Override
			public void actionPerformed(ActionEvent e) {
				popup.show(component, 10, 10);
			}
		});
  	
  }

}
