/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.jdesktop.swingx.JXTitledPanel;
import org.jdesktop.swingx.painter.Painter;

import com.jgoodies.binding.value.ValueModel;

/**
 * @author korf
 *
 */
public class TitledPanel extends JXTitledPanel {
	private static final long serialVersionUID = 1174481124130086654L;

	/**
	 * @param title
	 * @param content
	 */
	public TitledPanel(String title, Container content) {
		super(title != null ? title : " ", content);
		this.setTitlePainter(new Painter<Component>() {
		  private Color startColor = new Color(51, 90, 183);
		  private Color endColor = new Color(233, 233, 233);

			@Override
			public void paint(Graphics2D graphics, Component c, int width, int height) {
		    Graphics2D g = (Graphics2D) graphics;

		    GradientPaint gradient = new GradientPaint(0, 0, this.startColor, c.getWidth(), 0, this.endColor);
		    g.setPaint(gradient);
		    g.fill(new Rectangle2D.Double(0, 0, c.getWidth(), 20));
				
			}
			
		});
		this.setTitleFont(ComponentFactory.boldLabelFont);
		this.setTitleForeground(Color.WHITE);
	}

	public TitledPanel(ValueModel titleHolder, Container content) {
		this((String)titleHolder.getValue(), content);
		titleHolder.addValueChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if(evt.getNewValue() instanceof String) {
					setTitle((String)evt.getNewValue());
				}
				else {
					setTitle(" ");
				}
			}
			
			
		});
	}

}
