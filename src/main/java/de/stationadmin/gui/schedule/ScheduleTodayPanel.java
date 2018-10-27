/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.base.schedule.Schedule.Entry;
import de.stationadmin.gui.JumpHandler;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.JumpLabel;

/**
 * Displays todays schedule in a panel
 * 
 * @author Frank Korf
 */
public class ScheduleTodayPanel extends JPanel {
  private static final long serialVersionUID = 896809155860392655L;
  private JumpHandler jumpHandler;
  private Schedule schedule;
  private Color background = AppUtils.getTextBackgroundColor();

  public ScheduleTodayPanel(Schedule schedule, JumpHandler jumpHandler) {
    super();
    this.schedule = schedule;
    this.jumpHandler = jumpHandler;
    this.setLayout(new FormLayout("pref", "50dlu:grow"));
    this.setBackground(background);
    this.setOpaque(true);
    this.rebuild();

    schedule.addPropertyChangeListener("current", new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        rebuild();
      }
    });
  }

  private void rebuild() {
    this.removeAll();

    JPanel schedulePanel = new JPanel(new GridBagLayout());
    schedulePanel.setBackground(background);
    schedulePanel.setOpaque(true);

    List<Schedule.Entry> entries = this.schedule.getEffectiveEntriesOfToday();

    NumberFormat fmt = NumberFormat.getIntegerInstance();
    fmt.setMinimumIntegerDigits(2);

    Entry current = this.schedule.getCurrent();

    if (entries.size() > 0) {
      for (int i = 0; i < entries.size(); i++) {

        JLabel timeLabel = new JLabel(fmt.format(entries.get(i).getHour()) + ":00");
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

        Playlist playlist = this.schedule.getPlaylistRegistry().getPlaylist(entries.get(i).getPlaylistId());
        JLabel playlistLabel = new JumpLabel(this.jumpHandler, playlist != null ? playlist.getDisplayName() : "?") {
          private static final long serialVersionUID = -1066923129694795604L;

          /**
           * @see de.stationadmin.gui.util.JumpLabel#getJumpTarget()
           */
          @Override
          protected Object getJumpTarget() {
            return this.getClientProperty("playlist");
          }

        };
        playlistLabel.setOpaque(false);
        playlistLabel.putClientProperty("playlist", playlist);

        playlistLabel.addMouseListener(new MouseAdapter() {

          /**
           * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
           */
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
              if (e.getSource() instanceof JLabel) {
              }
            }
          }

        });

        if (entries.get(i).equals(current)) {
          timeLabel.setFont(ComponentFactory.boldLabelFont);
          playlistLabel.setFont(ComponentFactory.boldLabelFont);
        }

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.gridy = i;
        cc.anchor = GridBagConstraints.EAST;
        cc.insets = new Insets(5, 5, 10, 15);

        schedulePanel.add(timeLabel, cc);

        cc = new GridBagConstraints();
        cc.gridx = 1;
        cc.gridy = i;
        cc.anchor = GridBagConstraints.WEST;
        cc.insets = new Insets(5, 5, 10, 25);

        schedulePanel.add(playlistLabel, cc);

      }
    }

    JScrollPane scroll = new JScrollPane(schedulePanel);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    this.add(scroll, new CellConstraints(1, 1, CellConstraints.FILL, CellConstraints.TOP));

    this.invalidate();
    this.repaint();

  }

}
