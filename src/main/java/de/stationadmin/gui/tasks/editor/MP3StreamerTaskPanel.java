/**
 * 
 */
package de.stationadmin.gui.tasks.editor;

import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.io.FilenameUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.base.tools.MP3StreamerTask;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.live.FileSelectionAction;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;


/**
 * @author korf
 * 
 */
public class MP3StreamerTaskPanel extends JPanel implements ScheduledTaskEditorComponent {
  private static final long serialVersionUID = -700274115523588701L;
  private ValueHolder taskName = new ValueHolder();
  private ValueHolder sourcefile = new ValueHolder();
  private ValueHolder metafile = new ValueHolder();
  private ValueHolder maxDuration = new ValueHolder(0);
  private ValueHolder waitForNextTrack = new ValueHolder(Boolean.FALSE);

  private ValueHolder lastDir = new ValueHolder();
  private TextProvider textProvider;

  public MP3StreamerTaskPanel(TextProvider textProvider) {
    this.textProvider = textProvider;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("pref,5dlu,pref:grow,pref", "pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    final JTextField nameTf = BasicComponentFactory.createTextField(this.taskName);
    nameTf.setColumns(25);
    this.add(new JLabel(this.textProvider.getString("task.name")), cc.xy(1, 1));
    this.add(nameTf, cc.xywh(3, 1, 2, 1));

    final JTextField sourceTf = BasicComponentFactory.createTextField(this.sourcefile);
    sourceTf.setColumns(25);
    this.add(new JLabel(textProvider.getString("mp3streamer.dlg.property.source")), cc.xy(1, 3));
    this.add(sourceTf, cc.xy(3, 3));
    this.add(new JButton(new FileSelectionAction(this.sourcefile, this.lastDir, "mp3")), cc.xy(4, 3));

    this.sourcefile.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        String mp3 = (String) evt.getNewValue();
        if (mp3 != null && mp3.length() > 0) {
          String txt = FilenameUtils.removeExtension(mp3) + ".txt";
          if (new File(txt).exists()) {
            metafile.setValue(txt);
          }
        }
      }
    });

    final JTextField metaTf = BasicComponentFactory.createTextField(this.metafile);
    metaTf.setColumns(25);
    this.add(new JLabel(textProvider.getString("mp3streamer.dlg.property.meta")), cc.xy(1, 5));
    this.add(metaTf, cc.xy(3, 5));
    this.add(new JButton(new FileSelectionAction(this.metafile, this.lastDir, "txt")), cc.xy(4, 5));

    final JTextField durationTf = BasicComponentFactory.createIntegerField(this.maxDuration, 0);
    durationTf.setColumns(3);
    JPanel durationPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    durationPanel.add(durationTf);
    durationPanel.add(new JLabel(" " + textProvider.getString("mp3streamer.dlg.property.maxduration.unit")));
    this.add(new JLabel(textProvider.getString("mp3streamer.dlg.property.maxduration")), cc.xy(1, 7));
    this.add(durationPanel, cc.xy(3, 7, CellConstraints.LEFT, CellConstraints.CENTER));

    
    final JCheckBox delayCb = BasicComponentFactory.createCheckBox(this.waitForNextTrack, textProvider.getString("mp3streamer.dlg.property.waiting"));
    this.add(delayCb, cc.xywh(1, 9, 4, 1));
  }

  @Override
  public void updateView(ScheduledTask task) {
    if (task.getTask() instanceof MP3StreamerTask) {
      MP3StreamerTask mp3StreamerTask = (MP3StreamerTask) task.getTask();
      this.taskName.setValue(mp3StreamerTask.getName());
      this.sourcefile.setValue(mp3StreamerTask.getSourceFile());
      this.metafile.setValue(mp3StreamerTask.getMetaDataFile());
      this.waitForNextTrack.setValue(mp3StreamerTask.isWaitForTrackChange());
      this.maxDuration.setValue(mp3StreamerTask.getMaxDuration());
    }
  }

  @Override
  public void updateTask(ScheduledTask task) {
    if (task.getTask() instanceof MP3StreamerTask) {
      MP3StreamerTask mp3StreamerTask = (MP3StreamerTask) task.getTask();
      mp3StreamerTask.setName(this.taskName.getString());
      mp3StreamerTask.setSourceFile(this.sourcefile.getString());
      mp3StreamerTask.setMetaDataFile(this.metafile.getString());
      mp3StreamerTask.setWaitForTrackChange(this.waitForNextTrack.booleanValue());
      mp3StreamerTask.setMaxDuration(this.maxDuration.intValue());
    }

  }

}
