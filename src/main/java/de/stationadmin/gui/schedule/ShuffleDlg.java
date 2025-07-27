/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.schedule.ScheduleShuffler;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.SwingTools;

/**
 * @author korf
 *
 */
@SuppressWarnings("rawtypes")
public class ShuffleDlg extends JDialog {
  private static final long serialVersionUID = 7800810971400648712L;

  private ClientContext ctx;
  private ScheduleTableModel tableModel;

  private ValueHolder playlistFilter = new ValueHolder(ScheduleShuffler.TAG_USED);
  private ValueHolder entryFilter = new ValueHolder(null);
  private ValueHolder slotLengthRequired = new ValueHolder(Boolean.FALSE);

  /**
   * 
   */
  public ShuffleDlg(ClientContext ctx, ScheduleTableModel tableModel) {
    this.ctx = ctx;
    this.tableModel = tableModel;
    this.init();

  }

  private void init() {
  	this.setTitle(ctx.getTextProvider().getString("schedule.action.shuffle.name"));
    this.getContentPane().setLayout(new FormLayout("5dlu,pref,5dlu,pref,5dlu", "5dlu,pref,5dlu,pref,5dlu,pref,8dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    this.getContentPane().add(new JLabel(ctx.getString("schedule.action.shuffle.property.playlists")), cc.xy(2, 2));
    SelectionInList<String> model = new SelectionInList<String>(buildTagModel(), playlistFilter);
    JComboBox combo = BasicComponentFactory.createComboBox(model, new DefaultListCellRenderer() {
      private static final long serialVersionUID = -7901905125762119676L;

      /**
       * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
       */
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
          setText(ctx.getTextProvider().getString("playlistselector.all"));
        } else if (value.equals(ScheduleShuffler.TAG_USED)) {
          setText(ctx.getTextProvider().getString("playlistselector.used"));
        }
        return comp;
      }

    });
    this.getContentPane().add(combo, cc.xy(4, 2));
    
    this.getContentPane().add(new JLabel(ctx.getString("schedule.action.shuffle.property.entries")), cc.xy(2, 4));
    SelectionInList<String> entryModel = new SelectionInList<String>(buildEntryTagModel(), entryFilter);
    JComboBox entryCombo = BasicComponentFactory.createComboBox(entryModel, new DefaultListCellRenderer() {
      private static final long serialVersionUID = -7901905125762119676L;

      /**
       * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
       */
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
          setText(ctx.getTextProvider().getString("playlistselector.all"));
        } 
        return comp;
      }
    });
    this.getContentPane().add(entryCombo, cc.xy(4, 4));


    JCheckBox cb = BasicComponentFactory.createCheckBox(slotLengthRequired, ctx.getString("schedule.action.shuffle.property.slotLengthRequired"));
    this.getContentPane().add(cb, cc.xywh(2, 6, 3, 1));

    this.getContentPane().add(new JButton(new ShuffleAction()), cc.xywh(2, 8, 3, 1, CellConstraints.CENTER, CellConstraints.CENTER));
    
    Dimension dim = this.getPreferredSize();
    this.setSize((int)dim.getWidth() + 30, (int)dim.getHeight() + 50);
    SwingTools.centerWithin(ctx.getRootWindow(), this);

  }

  IndirectListModel<String> buildTagModel() {
    ArrayList<String> entries = new ArrayList<String>(ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getUsedTags());
    Collections.sort(entries);
    entries.add(0, null);
    entries.add(ScheduleShuffler.TAG_USED);
    return new IndirectListModel<String>(entries);
  }
  
  IndirectListModel<String> buildEntryTagModel() {
    ArrayList<String> entries = new ArrayList<String>(ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getUsedTags());
    Collections.sort(entries);
    entries.add(0, null);
    return new IndirectListModel<String>(entries);
  }


  private class ShuffleAction extends AbstractAction {
    private static final long serialVersionUID = -1708985373451632910L;

    ShuffleAction() {
      this.putValue(Action.NAME, ctx.getString("schedule.action.shuffle.name"));
    }

    @Override
    public void actionPerformed(ActionEvent evt) {

      ScheduleShuffler shuffler = new ScheduleShuffler(ctx.getAdminClient().getPlaylistService().getPlaylistRegistry(), ctx.getAdminClient().getSchedule().getBasePlaylist().getId());
      shuffler.setPlaylistTag(playlistFilter.getString());
      shuffler.setEntryTag(entryFilter.getString());
      shuffler.setSlotLenghForPlaylistsRequired((Boolean) slotLengthRequired.getValue());
      tableModel.setEntries(shuffler.shuffle(tableModel.getEntries()));

    }

  }
}
