/**
 * 
 */
package de.stationadmin.gui.upload;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.track.upload.QueuedTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.upload.UploadedTrackTableModel.Column;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;

/**
 * 
 * @author Frank Korf
 * 
 */
@SuppressWarnings("rawtypes")
class MultiTrackEditDlg extends JDialog {
  private static final long serialVersionUID = 566574505692346844L;
  private ClientContext ctx;
  private List<QueuedTrack> tracks;
  private TableModel tableModel;

  private JCheckBox artistEnabledCb;
  private JTextField artistTf;
  private JCheckBox titleEnabledCb;
  private JTextField titleTf;
  private JCheckBox albumEnabledCb;
  private JTextField albumTf;
  private JCheckBox genreEnabledCb;
  private JTextField genreTf;
  private JCheckBox yearEnabledCb;
  private JTextField yearTf;
  private JCheckBox typeEnabledCb;
  private JComboBox typeCmb;
//  private JCheckBox privateEnabledCb;
//  private JCheckBox privateCb;

  public MultiTrackEditDlg(ClientContext ctx, List<QueuedTrack> tracks, TableModel model) {
    super();
    this.ctx = ctx;
    this.tracks = tracks;
    this.tableModel = model;
    this.init();
  }

  private void init() {
    this.setTitle(ctx.getString("upload.multiedit.title"));
    
    StringBuilder rowSpec = new StringBuilder();
    rowSpec.append("5dlu,");
    rowSpec.append("pref,5dlu,"); // artist
    rowSpec.append("pref,5dlu,"); // title
    rowSpec.append("pref,5dlu,"); // album
    rowSpec.append("pref,5dlu,"); // genre
    rowSpec.append("pref,5dlu,"); // year
    rowSpec.append("pref,5dlu,"); // type
    rowSpec.append("pref,8dlu:grow,"); // private
    rowSpec.append("pref,5dlu:grow,"); // buttons

    this.setLayout(new FormLayout("5dlu,pref,5dlu,pref,5dlu,pref:grow,5dlu", rowSpec.toString()));

    int row = 2;

    {
      this.artistEnabledCb = new JCheckBox();
      this.artistTf = new JTextField(20);
      this.addRow(Column.ARTIST, this.artistEnabledCb, this.artistTf, row);
      row += 2;
    }

    {
      this.titleEnabledCb = new JCheckBox();
      this.titleTf = new JTextField(20);
      this.addRow(Column.TITLE, this.titleEnabledCb, this.titleTf, row);
      row += 2;
    }

    {
      this.albumEnabledCb = new JCheckBox();
      this.albumTf = new JTextField(20);
      this.addRow(Column.ALBUM, this.albumEnabledCb, this.albumTf, row);
      row += 2;
    }

    {
      this.genreEnabledCb = new JCheckBox();
      this.genreTf = new JTextField(20);
      this.addRow(Column.GENRE, this.genreEnabledCb, this.genreTf, row);
      row += 2;
    }

    {
      this.yearEnabledCb = new JCheckBox();
      this.yearTf = new JTextField(4);
      this.addRow(Column.YEAR, this.yearEnabledCb, this.yearTf, row);
      row += 2;
    }

    {
      this.typeEnabledCb = new JCheckBox();
      this.typeCmb = new JComboBox<Integer>(new Integer[] { 1, 2, 3 });
      this.typeCmb.setRenderer(new TrackTypeListCellRenderer(ctx));
      this.addRow(Column.TYPE, this.typeEnabledCb, this.typeCmb, row);
      row += 2;
    }

//    {
//      this.privateEnabledCb = new JCheckBox();
//      this.privateCb = new JCheckBox();
//      this.addRow(Column.PRIVATE, this.privateEnabledCb, this.privateCb, row);
//      row += 2;
//    }

    JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 10));
    JButton ok = new JButton(new ApplyAction());
    btnPanel.add(ok);
    JButton cancel = new JButton(new DisposeAction(this, ctx.getString("cancel")));
    btnPanel.add(cancel);
    
    this.add(btnPanel, new CellConstraints(2, row, 5, 1, CellConstraints.CENTER, CellConstraints.CENTER));

    Dimension dim = this.getPreferredSize();
    this.setSize(dim.width + 50, dim.height + 50);
    SwingTools.centerWithin(ctx.getRootWindow(), this);

  }

  private void addRow(Column col, JCheckBox cb, JComponent comp, int row) {
    this.add(cb, new CellConstraints(2, row));
    JLabel label = new JLabel(ctx.getString("upload.title.column." + col.name().toLowerCase()));
    this.add(label, new CellConstraints(4, row));
    this.add(comp, new CellConstraints(6, row));
    setEnabled(comp, false);
    cb.addActionListener(new ComponentEnabler(cb, comp));

  }

  private static void setEnabled(JComponent comp, boolean enabled) {
    if (comp instanceof JTextComponent) {
      ((JTextComponent) comp).setEditable(enabled);
    } else {
      comp.setEnabled(enabled);
    }
  }

  private static class ComponentEnabler implements ActionListener {
    private JCheckBox cb;
    private JComponent comp;

    public ComponentEnabler(JCheckBox cb, JComponent comp) {
      super();
      this.cb = cb;
      this.comp = comp;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      setEnabled(comp, cb.isSelected());
    }

  }

  private void apply() {
    for (QueuedTrack track : tracks) {

      if (this.artistEnabledCb.isSelected()) {
        track.getTrack().setArtist(this.artistTf.getText());
        track.setModified(true);
      }
      if (this.titleEnabledCb.isSelected()) {
        track.getTrack().setTitle(this.titleTf.getText());
        track.setModified(true);
      }
      if (this.albumEnabledCb.isSelected()) {
        track.getTrack().setAlbum(this.albumTf.getText());
        track.setModified(true);
      }
      if (this.genreEnabledCb.isSelected()) {
        track.getTrack().setGenre(this.genreTf.getText());
        track.setModified(true);
      }
      if (this.yearEnabledCb.isSelected()) {
        try {
          int year = Integer.parseInt(this.yearTf.getText());
          track.getTrack().setYear(year);
          track.setModified(true);
        } catch (NumberFormatException e) {
        }
      }
      if (this.typeEnabledCb.isSelected()) {
        track.getTrack().setType((Integer)typeCmb.getSelectedItem());
        track.setModified(true);
      }
//      if (this.privateEnabledCb.isSelected()) {
//        title.setPrivateTrack(this.privateCb.isSelected());
//      }

    }

  }

  private class ApplyAction extends AbstractAction {
    private static final long serialVersionUID = -2023605060309701900L;

    ApplyAction() {
      this.putValue(Action.NAME, ctx.getString("ok"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      apply();
      ((AbstractTableModel) tableModel).fireTableDataChanged();
      dispose();

    }

  }

}
