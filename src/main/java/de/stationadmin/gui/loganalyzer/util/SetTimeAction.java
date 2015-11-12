/**
 * 
 */
package de.stationadmin.gui.loganalyzer.util;

import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.base.loganalyzer.LogAnalyzerService;
import de.stationadmin.gui.TextProvider;

public class SetTimeAction extends AbstractAction {
  private static final long serialVersionUID = -7108152957937441940L;
  private TimeEditor fromTime;
  private TimeEditor toTime;
  int mode;

  public SetTimeAction(TextProvider textProvider, TimeEditor from, TimeEditor to, int mode) {
    super();
    this.mode = mode;
    this.fromTime = from;
    this.toTime = to;
    this.putValue(Action.NAME, textProvider.getString("playsanalyzer.filter.timeframe." + mode));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    Date toTime = new Date();
    Date fromTime;
    switch(mode) {
    case 0:
      fromTime = new Date(System.currentTimeMillis() - LogAnalyzerService.DAY_IN_MS);
      break;
    case 1:
      fromTime = new Date(System.currentTimeMillis() - LogAnalyzerService.DAY_IN_MS * 7);
      break;
    default:
      Calendar cal = Calendar.getInstance();
      cal.setTime(toTime);
      cal.add(Calendar.MONTH, -1);
      fromTime = cal.getTime();
    }
    
    this.fromTime.update(fromTime);
    this.toTime.update(toTime);
    
  }
  
}