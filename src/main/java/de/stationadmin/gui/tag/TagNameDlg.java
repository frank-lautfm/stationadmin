package de.stationadmin.gui.tag;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;

public class TagNameDlg extends JDialog {
  private static final long serialVersionUID = 5114286443127515735L;
  private boolean accepted = false;
  private TextProvider textProvider;
  
  private static Integer[] days = new Integer[] {
  	1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
  	21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
  };
  
  private static Integer[] months = new Integer[] {
    	1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12
    };

  
  private ValueHolder tagName = new ValueHolder();
  private ValueHolder tagType = new ValueHolder("standard");

  private Component currentEditor;
  

  public TagNameDlg(TextProvider txtProvider) {
    this.setTitle("Tag");
    this.textProvider = txtProvider;
    this.initalize();
  }
  
  @SuppressWarnings("rawtypes")
  private void initalize() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref,5dlu,pref,5dlu"));

    String[] tagTypes = new String[] { "standard", "date", "grouping" };
    SelectionInList<String> tagTypeSelection = new SelectionInList<String>(tagTypes, this.tagType);
    JComboBox tagTypeCmb = BasicComponentFactory.createComboBox(tagTypeSelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = 3817876613492656987L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setText(textProvider.getString("titletagmanager.tagtype." + value));
        return comp;
      }
      
    });
    this.getContentPane().add(tagTypeCmb, new CellConstraints(2,2));
    
    currentEditor = createStandardPanel();
		getContentPane().add(currentEditor, new CellConstraints(2,4,CellConstraints.FILL,CellConstraints.FILL));
    
    
    this.tagType.addValueChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String value = tagType.getString();
				Component newPanel = null;
				if(value.equals("date")) {
					newPanel = createDatePanel();
				}
				else if(value.equals("grouping")) {
					newPanel = createGroupingPanel();
				}
				else {
					newPanel = createStandardPanel();
				}
				
				
				getContentPane().remove(currentEditor);
				getContentPane().add(newPanel, new CellConstraints(2,4,CellConstraints.FILL,CellConstraints.FILL));
				currentEditor = newPanel;
				
				validate();
				repaint();
			}
		});
        
    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    JButton okBtn = new JButton("Ok");
    okBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        String name = tagName.getString();
        if (name.length() == 0) {
          ErrorInfo errorInfo = textProvider.createErrorInfo(null, "titletagmanager.action.save.illegalname.empty");
          JXErrorPane.showDialog(null, errorInfo);
        } else if (name.contains("/")) {
          ErrorInfo errorInfo = textProvider.createErrorInfo(null, "titletagmanager.action.save.illegalname.slash");
          JXErrorPane.showDialog(null, errorInfo);
        } else {

          accepted = true;
          dispose();
        }
      }

    });

    buttonPanel.add(okBtn);
    buttonPanel.add(new JButton(new DisposeAction(this, "Abbruch"))); // FIXME
    // localize
    this.getContentPane().add(buttonPanel, new CellConstraints(2, 6, CellConstraints.CENTER, CellConstraints.CENTER));

    this.pack();
    int w = getSize().width;
    int h = getSize().height;
    this.setSize(w + 100, h);
    
    SwingTools.centerOnScreen(this);
  	
  }
  
  private JPanel createStandardPanel() {
  	JPanel panel = new JPanel(new FormLayout("pref:grow", "pref"));
  	
  	ValueHolder text = new ValueHolder();
  	JTextField tf = BasicComponentFactory.createTextField(text);
  	tf.setColumns(20);
  	panel.add(tf, new CellConstraints(1, 1));
  	
  	text.addValueChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				tagName.setValue(text.getValue());
			}
		});
  	
  	return panel;
  }
  
  @SuppressWarnings("rawtypes")
  private JPanel createDatePanel() {
  	JPanel panel = new JPanel(new FormLayout("pref,pref,pref,1dlu,pref,1dlu,pref,pref,1dlu,pref:grow", "pref"));
  	CellConstraints cc = new CellConstraints();
  	panel.add(new JLabel("@"), cc.xy(1, 1));

  	ValueHolder fromDay = new ValueHolder(1);
  	SelectionInList<Integer> fromDaySelection = new SelectionInList<Integer>(days, fromDay);
  	JComboBox fromDayCmb = BasicComponentFactory.createComboBox(fromDaySelection);
  	panel.add(fromDayCmb, cc.xy(2, 1));

  	ValueHolder fromMonth = new ValueHolder(1);
  	SelectionInList<Integer> fromMonthSelection = new SelectionInList<Integer>(months, fromMonth);
  	JComboBox fromMonthCmb = BasicComponentFactory.createComboBox(fromMonthSelection);
  	panel.add(fromMonthCmb, cc.xy(3, 1));

  	panel.add(new JLabel("-"), cc.xy(5, 1));

  	ValueHolder toDay = new ValueHolder(1);
  	SelectionInList<Integer> toDaySelection = new SelectionInList<Integer>(days, toDay);
  	JComboBox toDayCmb = BasicComponentFactory.createComboBox(toDaySelection);
  	panel.add(toDayCmb, cc.xy(7, 1));

  	ValueHolder toMonth = new ValueHolder(1);
  	SelectionInList<Integer> toMonthSelection = new SelectionInList<Integer>(months, toMonth);
  	JComboBox toMonthCmb = BasicComponentFactory.createComboBox(toMonthSelection);
  	panel.add(toMonthCmb, cc.xy(8, 1));

  	ValueHolder text = new ValueHolder();
  	JTextField tf = BasicComponentFactory.createTextField(text);
  	tf.setColumns(10);
  	panel.add(tf, cc.xy(10, 1));
  	
  	PropertyChangeListener listener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				
				StringBuffer buf = new StringBuffer();
				buf.append("@");
				buf.append(intToString(fromDay));
				buf.append(".");
				buf.append(intToString(fromMonth));
				buf.append(".");
				buf.append("-");
				buf.append(intToString(toDay));
				buf.append(".");
				buf.append(intToString(toMonth));
				buf.append(".");
				if(text.getString() != null && text.getString().length() > 0) {
					buf.append(" ");
					buf.append(text.getString());
				}
				
				tagName.setValue(buf.toString());
				
			}
		};
		
		fromDay.addValueChangeListener(listener);
		toDay.addValueChangeListener(listener);
		fromMonth.addValueChangeListener(listener);
		toMonth.addValueChangeListener(listener);
		text.addValueChangeListener(listener);
  	
  	return panel;
  }
  
  private String intToString(ValueHolder valueHolder) {
    Integer value = (Integer) valueHolder.getValue();
    return (value != null) ? String.format("%02d", value) : "00";
  }
  
  private JPanel createGroupingPanel() {
  	JPanel panel = new JPanel(new FormLayout("pref,pref:grow", "pref"));
  	panel.add(new JLabel("="), new CellConstraints(1, 1));
  	
  	ValueHolder text = new ValueHolder();
  	JTextField tf = BasicComponentFactory.createTextField(text);
  	tf.setColumns(20);
  	panel.add(tf, new CellConstraints(2, 1));
  	
  	text.addValueChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				tagName.setValue("=" + text.getString());
			}
		});
  	
  	return panel;
  }


  /**
   * @return the accepted
   */
  public boolean isAccepted() {
    return accepted;
  }

  public String getTagName() {
    return StringUtils.trimToNull(this.tagName.getString());
  }
}
