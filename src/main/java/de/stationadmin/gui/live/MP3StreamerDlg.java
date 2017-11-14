/**
 * 
 */
package de.stationadmin.gui.live;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.io.FilenameUtils;
import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.LiveAccount;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.streamlive.MP3FileSource;
import de.stationadmin.streamlive.MP3Source;
import de.stationadmin.streamlive.MP3Streamer;
import de.stationadmin.streamlive.MP3Streamer.Status;
import de.stationadmin.streamlive.MP3URLSource;

/**
 * @author korf
 * 
 */
public class MP3StreamerDlg extends StationAdminFrame {
  private static final long serialVersionUID = 2040651084297013845L;

  private ValueHolder sourcefile = new ValueHolder();
  private ValueHolder metafile = new ValueHolder();
  private ValueHolder maxDuration = new ValueHolder(0);
  private ValueHolder waitForNextTrack = new ValueHolder(Boolean.FALSE);
  private ValueHolder configEnabled = new ValueHolder(Boolean.TRUE);

  private ValueHolder status = new ValueHolder("offline");
  private ValueHolder time = new ValueHolder("0:00");
  private ValueHolder track = new ValueHolder(null);

  private ValueModel lastDir = new ValueHolder();

  private Timer timer;

  /**
   * @param ctx
   * @throws HeadlessException
   */
  public MP3StreamerDlg(ClientContext ctx) throws HeadlessException {
    super(ctx, "MP3Streamer");
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,10dlu,pref,10dlu,pref,5dlu"));
    this.setTitle(ctx.getTextProvider().getString("mp3streamer.dlg.title"));
    if (ctx.getAdminClient().getMp3Streamer() != null) {
      this.sourcefile.setValue(ctx.getAdminClient().getMp3Streamer().getSource().getLocation());
      if (ctx.getAdminClient().getMp3Streamer().getMeta() != null) {
        this.metafile.setValue(ctx.getAdminClient().getMp3Streamer().getMeta().getAbsolutePath());
      }
    }
    CellConstraints cc = new CellConstraints();
    this.getContentPane().add(createConfigPanel(), cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));
    this.getContentPane().add(createStatusPanel(), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));
    this.getContentPane().add(createButtonPanel(), cc.xy(2, 6, CellConstraints.CENTER, CellConstraints.CENTER));

    this.timer = new Timer(1000, new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        checkStatus();
      }
    });
    this.timer.start();
  }

  private void checkStatus() {
    if (this.ctx.getAdminClient().getMp3Streamer() != null) {
      this.configEnabled.setValue(this.ctx.getAdminClient().getMp3Streamer().getStatus() == Status.OFFLINE);
      this.status.setValue(ctx.getTextProvider().getString(
          "mp3streamer.status." + this.ctx.getAdminClient().getMp3Streamer().getStatus().name().toLowerCase()));
      this.time.setValue(TimeFormat.format(this.ctx.getAdminClient().getMp3Streamer().getPlayTime(), false));
      if (this.ctx.getAdminClient().getMp3Streamer().getNumTracks() > 0) {
        if (this.ctx.getAdminClient().getMp3Streamer().getCurrentTrackIndex() > -1) {
          String progress = " (" + (this.ctx.getAdminClient().getMp3Streamer().getCurrentTrackIndex() + 1) + " / "
              + this.ctx.getAdminClient().getMp3Streamer().getNumTracks() + ")";
          this.track.setValue(this.ctx.getAdminClient().getMp3Streamer().getCurrentSong() + progress);
        } else {
          this.track.setValue(this.ctx.getAdminClient().getMp3Streamer().getCurrentSong());
        }
      }
    } else {
      this.configEnabled.setValue(true);
      this.status.setValue(ctx.getTextProvider().getString("mp3streamer.status.offline"));
      this.time.setValue("0:00");
      this.track.setValue(null);
    }

  }

  private JPanel createConfigPanel() {
    JPanel panel = new JPanel(new FormLayout("5dlu,pref,5dlu,pref:grow,pref,5dlu", "5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    final JTextField sourceTf = ctx.getComponentFactory().createTextField(this.sourcefile);
    sourceTf.setColumns(25);
    panel.add(new JLabel(this.ctx.getString("mp3streamer.dlg.property.source")), cc.xy(2, 2));
    panel.add(sourceTf, cc.xy(4, 2));
    panel.add(new JButton(new FileSelectionAction(this.sourcefile, this.lastDir, "mp3")), cc.xy(5, 2));

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

    final JTextField metaTf = ctx.getComponentFactory().createTextField(this.metafile);
    metaTf.setColumns(25);
    panel.add(new JLabel(this.ctx.getString("mp3streamer.dlg.property.meta")), cc.xy(2, 4));
    panel.add(metaTf, cc.xy(4, 4));
    panel.add(new JButton(new FileSelectionAction(this.metafile, this.lastDir, "txt")), cc.xy(5, 4));
    
    final JTextField durationTf = BasicComponentFactory.createIntegerField(this.maxDuration, 0);
    durationTf.setColumns(3);
    JPanel durationPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    durationPanel.add(durationTf);
    durationPanel.add(new JLabel(" " + this.ctx.getString("mp3streamer.dlg.property.maxduration.unit")));
    panel.add(new JLabel(this.ctx.getString("mp3streamer.dlg.property.maxduration")), cc.xy(2, 6));
    panel.add(durationPanel, cc.xy(4, 6, CellConstraints.LEFT, CellConstraints.CENTER));


    final JCheckBox delayCb = BasicComponentFactory.createCheckBox(this.waitForNextTrack, this.ctx.getString("mp3streamer.dlg.property.waiting"));
    panel.add(delayCb, cc.xywh(2, 8, 4, 1));

    this.configEnabled.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        Boolean b = (Boolean) evt.getNewValue();
        sourceTf.setEditable(b.booleanValue());
        metaTf.setEditable(b.booleanValue());
        durationTf.setEditable(b.booleanValue());
        delayCb.setEnabled(b.booleanValue());
      }
    });

    panel.setBorder(BorderFactory.createTitledBorder((String) null));
    return panel;
  }

  private JPanel createStatusPanel() {
    JPanel panel = new JPanel(new FormLayout("5dlu,pref,5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref,5dlu,pref,5dlu"));
    panel.setBorder(BorderFactory.createTitledBorder((String) null));
    CellConstraints cc = new CellConstraints();

    panel.add(new JLabel(this.ctx.getString("mp3streamer.dlg.property.status")), cc.xy(2, 2));
    panel.add(BasicComponentFactory.createLabel(this.status), cc.xy(4, 2));

    panel.add(new JLabel(this.ctx.getString("mp3streamer.dlg.property.time")), cc.xy(2, 4));
    panel.add(BasicComponentFactory.createLabel(this.time), cc.xy(4, 4));

    panel.add(new JLabel(this.ctx.getString("mp3streamer.dlg.property.title")), cc.xy(2, 6));
    panel.add(BasicComponentFactory.createLabel(this.track), cc.xy(4, 6));

    return panel;
  }

  private JPanel createButtonPanel() {
    JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));

    panel.add(new JButton(new StartAction()));
    panel.add(new JButton(new StopAction()));

    return panel;
  }

  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(400, 300);
  }

  private class StartAction extends AbstractAction {
    private static final long serialVersionUID = -3613584642094506078L;

    StartAction() {
      this.putValue(Action.NAME, ctx.getTextProvider().getString("mp3streamer.dlg.action.start.name"));
      this.setEnabled(configEnabled.booleanValue());
      configEnabled.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          setEnabled(configEnabled.booleanValue());
        }
      });
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      String sourceStr = sourcefile.getString();
      MP3Source source = null;
      if (new File(sourceStr).exists()) {
        source = new MP3FileSource(new File(sourceStr));
      } else if (sourceStr != null && sourceStr.toLowerCase().startsWith("http")) {
        source = new MP3URLSource(sourceStr);
      }
      File meta = metafile.getValue() != null ? new File(metafile.getString()) : null;
      if (source != null) {
        try {
          final MP3Streamer streamer = new MP3Streamer(source, meta != null && meta.exists() ? meta : null);
          streamer.setMaxDuration((Integer)maxDuration.getValue());
          LiveAccount account = ctx.getAdminClient().getLiveAccount();
          streamer.configureServer(account.getServer(), account.getPort(), ctx.getAdminClient().getStation(), account.getUser(),
              account.getPassword());
          ctx.getAdminClient().setMp3Streamer(streamer);
          Thread t = new Thread() {
            public void run() {
              try {
                streamer.run(waitForNextTrack.booleanValue());
              } catch (Exception e) {
                e.printStackTrace();
                final Exception ex = e;
                SwingUtilities.invokeLater(new Runnable() {
                  @Override
                  public void run() {
                    JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(ex, "mp3streamer.dlg.action.start.error"));
                  }
                });
              }
            }
          };
          t.setDaemon(true);
          t.start();
        } catch (IOException e) {
          e.printStackTrace();
          JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "mp3streamer.dlg.action.start.error"));
        }

        checkStatus();
      }
    }

  }

  private class StopAction extends AbstractAction {
    private static final long serialVersionUID = 5173790937197854130L;

    StopAction() {
      this.putValue(Action.NAME, ctx.getTextProvider().getString("mp3streamer.dlg.action.stop.name"));
      this.setEnabled(!configEnabled.booleanValue());
      configEnabled.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          setEnabled(!configEnabled.booleanValue());
        }
      });
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      if (ctx.getAdminClient().getMp3Streamer() != null && ctx.getAdminClient().getMp3Streamer().getStatus() != Status.OFFLINE) {
        ctx.getAdminClient().getMp3Streamer().abort();
        checkStatus();
      }
    }

  }

}
