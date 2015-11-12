/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXLabel;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackAlias;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.SwingTools;

/**
 * 
 * @author Frank Korf
 * 
 */
public class TrackAliasManager extends JFrame {
  private static final long serialVersionUID = 7324117098950770350L;
  private ClientContext ctx;
  private TextProvider textProvider;
  private IndirectListModel<TrackAliasModel> aliasListModel;
  private ValueHolder selectedAlias = new ValueHolder(null, true);

  public TrackAliasManager(ClientContext ctx) throws HeadlessException {
    super();
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.init();
  }

  private void init() {
    this.setTitle(textProvider.getString("titlealiasmanager.title"));
    this.getContentPane().setLayout(new BorderLayout());

    {
      String desc = textProvider.getString("titlealiasmanager.description");
      JXLabel label = new JXLabel(desc);
      label.setLineWrap(true);
      label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      this.add(label, BorderLayout.NORTH);
    }

    {
      this.aliasListModel = new IndirectListModel<TrackAliasModel>(this.getAliasList());
      final JList list = new JList(this.aliasListModel);
      list.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            selectedAlias.setValue(list.getSelectedValue());
          }
        }

      });

      this.getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);
    }

    {
      PresentationModel<TrackAliasModel> aliasPm = new PresentationModel<TrackAliasModel>(this.selectedAlias) {
        private static final long serialVersionUID = 6069483280214585877L;

        /**
         * @see com.jgoodies.binding.PresentationModel#beforeBeanChange(java.lang.Object,
         *      java.lang.Object)
         */
        @Override
        public void beforeBeanChange(TrackAliasModel oldBean, TrackAliasModel newBean) {
          this.triggerFlush();
        }

      };
      JPanel detailFrame = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref,5dlu,pref,5dlu,pref,3dlu"));
      CellConstraints cc = new CellConstraints();

      {
        JPanel titlePanel = new JPanel(new FormLayout("2dlu,pref,5dlu,pref:grow,2dlu", "2dlu,pref,5dlu,pref,2dlu"));

        titlePanel.add(new JLabel(textProvider.getString("titlealiasmanager.property.artist") + ":"), cc.xy(2, 2));
        JTextField artistTf = BasicComponentFactory.createTextField(aliasPm.getBufferedModel("titleArtist"));
        artistTf.setEditable(false);
        titlePanel.add(artistTf, cc.xy(4, 2));

        titlePanel.add(new JLabel(textProvider.getString("titlealiasmanager.property.title") + ":"), cc.xy(2, 4));
        JTextField titleTf = BasicComponentFactory.createTextField(aliasPm.getBufferedModel("titleName"));
        titleTf.setEditable(false);
        titlePanel.add(titleTf, cc.xy(4, 4));

        titlePanel.setBorder(BorderFactory.createTitledBorder(textProvider.getString("titlealiasmanager.section.title")));
        detailFrame.add(titlePanel, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));
      }

      {
        JPanel aliasPanel = new JPanel(new FormLayout("2dlu,pref,5dlu,pref:grow,2dlu", "2dlu,pref,5dlu,pref,2dlu"));

        aliasPanel.add(new JLabel(textProvider.getString("titlealiasmanager.property.artist") + ":"), cc.xy(2, 2));
        final JTextField artistTf = BasicComponentFactory.createTextField(aliasPm.getBufferedModel("aliasArtist"),
            false);
        artistTf.setEditable(false);
        aliasPanel.add(artistTf, cc.xy(4, 2));

        aliasPanel.add(new JLabel(textProvider.getString("titlealiasmanager.property.title") + ":"), cc.xy(2, 4));
        final JTextField titleTf = BasicComponentFactory.createTextField(aliasPm.getBufferedModel("aliasName"), false);
        titleTf.setEditable(false);
        aliasPanel.add(titleTf, cc.xy(4, 4));

        aliasPanel.setBorder(BorderFactory.createTitledBorder(textProvider.getString("titlealiasmanager.section.alias")));
        detailFrame.add(aliasPanel, cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

        this.selectedAlias.addValueChangeListener(new PropertyChangeListener() {

          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            artistTf.setEditable(evt.getNewValue() != null);
            titleTf.setEditable(evt.getNewValue() != null);
          }
        });
      }

      {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 5, 5));
        buttonPanel.add(new JButton(new UpdateAction(aliasPm)));
        buttonPanel.add(new JButton(new DeleteAction()));

        detailFrame.add(buttonPanel, cc.xy(2, 6, CellConstraints.CENTER, CellConstraints.CENTER));
      }

      this.getContentPane().add(detailFrame, BorderLayout.SOUTH);

    }

    this.setSize(500, 500);
    SwingTools.centerWithin(ctx.getRootWindow(), this);

  }

  private List<TrackAliasModel> getAliasList() {
    List<TrackAliasModel> list = new ArrayList<TrackAliasModel>();
    for (RegisteredTrack title : this.ctx.getAdminClient().getTitleRegistry().getAllTracks()) {
      if (title.getAliases() != null) {
        for (TrackAlias alias : title.getAliases()) {
          list.add(new TrackAliasModel(title, alias));
        }
      }
    }
    Collections.sort(list);
    return list;
  }

  private class UpdateAction extends AbstractAction {
    private static final long serialVersionUID = -1199609359149005589L;
    private PresentationModel<TrackAliasModel> model;

    UpdateAction(PresentationModel<TrackAliasModel> model) {
      super(textProvider.getString("change"));
      this.model = model;

      PropertyChangeListener pcs = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          checkEnabled();
        }

      };

      this.model.getBeanChannel().addValueChangeListener(pcs);
      this.model.addPropertyChangeListener("buffering", pcs);
      this.checkEnabled();
    }

    private void checkEnabled() {
      this.setEnabled(this.model.getBean() != null && this.model.isBuffering());
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      this.model.triggerCommit();
      try {
        ctx.getAdminClient().getTrackService().saveAliases();
      } catch (Exception e) {
      }
      this.checkEnabled();
    }

  }

  private class DeleteAction extends AbstractAction {
    private static final long serialVersionUID = -7689941956682319526L;

    DeleteAction() {
      super(textProvider.getString("delete"));
      this.setEnabled(false);
      selectedAlias.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          setEnabled(evt.getNewValue() != null);
        }

      });

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      if (selectedAlias != null) {
        TrackAliasModel model = (TrackAliasModel) selectedAlias.getValue();
        model.getTitle().removeAlias(model.getAlias());
        try {
          ctx.getAdminClient().getTrackService().saveAliases();
        } catch (Exception e) {
        }
        List<TrackAliasModel> newList = new ArrayList<TrackAliasModel>(aliasListModel.getList());
        newList.remove(model);
        aliasListModel.setList(newList);
      }
    }

  }
}
