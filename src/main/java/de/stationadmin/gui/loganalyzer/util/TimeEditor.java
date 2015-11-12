/**
 * 
 */
package de.stationadmin.gui.loganalyzer.util;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JTextField;

import com.toedter.calendar.JDateChooser;

import de.stationadmin.gui.TextProvider;

public class TimeEditor {
  private JDateChooser dateChooser;
  private JTextField timeTf;
  private SimpleDateFormat timeFormat;

  public TimeEditor(TextProvider textProvider, Date date) {

    this.dateChooser = new JDateChooser();
    this.dateChooser.setLocale(textProvider.getLocale());
    this.dateChooser.setDateFormatString(textProvider.getString("dateFormat"));
    this.timeFormat = new SimpleDateFormat(textProvider.getString("timeOnlyFormat"));

    this.timeTf = new JTextField(5);
    this.timeTf.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent evt) {
        try {
          timeFormat.parse(timeTf.getText());
          timeTf.setBackground(Color.WHITE);
        } catch (Exception e) {
          Toolkit.getDefaultToolkit().beep();
          timeTf.setBackground(new Color(255, 200, 200));
        }

      }

      @Override
      public void focusGained(FocusEvent e) {

      }
    });

    this.update(date);
  }

  public void update(Date date) {
    this.dateChooser.setDate(date);
    this.timeTf.setText(this.timeFormat.format(date));
  }

  public Date getDate() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(this.dateChooser.getDate());
    
    try {
      Calendar cal2 = Calendar.getInstance();
      Date time = timeFormat.parse(timeTf.getText());
      cal2.setTime(time);
      cal.set(Calendar.HOUR_OF_DAY, cal2.get(Calendar.HOUR_OF_DAY));
      cal.set(Calendar.MINUTE, cal2.get(Calendar.MINUTE));
      
    } catch(Exception e) {
      
    }
    
    return cal.getTime();
  }

  public JDateChooser getDateChooser() {
    return dateChooser;
  }

  public JTextField getTimePanel() {

    return this.timeTf;
  }

}