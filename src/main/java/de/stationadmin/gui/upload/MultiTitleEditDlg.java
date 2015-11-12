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

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.upload.UploadedTitleTableModel.Column;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;

/**
 * 
 * @author Frank Korf
 * 
 */
class MultiTitleEditDlg extends JDialog {
  private ClientContext ctx;
  private List<DetailedTrack> titles;
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
  private JCheckBox privateEnabledCb;
  private JCheckBox privateCb;

  public MultiTitleEditDlg(ClientContext ctx, List<DetailedTrack> titles, TableModel model) {
    super();
    this.ctx = ctx;
    this.titles = titles;
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
      this.typeCmb = new JComboBox(new Integer[] { 1, 2, 3 });
      this.typeCmb.setRenderer(new TitleTypeListCellRenderer(ctx));
      this.addRow(Column.TYPE, this.typeEnabledCb, this.typeCmb, row);
      row += 2;
    }

    {
      this.privateEnabledCb = new JCheckBox();
      this.privateCb = new JCheckBox();
      this.addRow(Column.PRIVATE, this.privateEnabledCb, this.privateCb, row);
      row += 2;
    }

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
    for (DetailedTrack title : titles) {

      if (this.artistEnabledCb.isSelected()) {
        title.setArtist(this.artistTf.getText());
      }
      if (this.titleEnabledCb.isSelected()) {
        title.setTitle(this.titleTf.getText());
      }
      if (this.albumEnabledCb.isSelected()) {
        title.setAlbum(this.albumTf.getText());
      }
      if (this.genreEnabledCb.isSelected()) {
        title.setGenre(this.genreTf.getText());
      }
      if (this.yearEnabledCb.isSelected()) {
        try {
          int year = Integer.parseInt(this.yearTf.getText());
          title.setYear(year);
        } catch (NumberFormatException e) {
        }
      }
      if (this.typeEnabledCb.isSelected()) {
        title.setType((Integer)typeCmb.getSelectedItem());
      }
      if (this.privateEnabledCb.isSelected()) {
        title.setPrivateTrack(this.privateCb.isSelected());
      }

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
