/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.track.SearchResultSet;
import de.stationadmin.base.track.Title;
import de.stationadmin.base.track.TrackQuery;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.NonObservingPresentationModel;

/**
 * 
 * @author Frank Korf
 * 
 */
public class SearchPanel extends JPanel {
  private static final long serialVersionUID = -7537959794028736643L;

  private ClientContext ctx;
  private PresentationModel<TrackQuery> model = new NonObservingPresentationModel<TrackQuery>(new TrackQuery());
  private ValueModel searchResultHolder = new ValueHolder(null, true);
  private ValueModel selectionHolder = new ValueHolder(new ArrayList<Title>(0), true);

  private ValueHolder hitCount = new ValueHolder(0);
  private ValueHolder pageNum = new ValueHolder(1);
  private ValueHolder pageCount = new ValueHolder(1);

  public SearchPanel(ClientContext ctx) {
    this(ctx, true);
  }

  public SearchPanel(ClientContext ctx, boolean multiSelection) {
    super();
    this.ctx = ctx;
    this.init(multiSelection);
  }

  private void init(boolean multiSelection) {
    this.setLayout(new BorderLayout());
    SearchResultViewer viewer =new SearchResultViewer(ctx, model, this.searchResultHolder, this.selectionHolder, multiSelection);
    viewer.setSearchAction(new Search());
    this.add(viewer, BorderLayout.CENTER);
    this.add(this.createBottomPanel(), BorderLayout.SOUTH);
  }

  private JPanel createBottomPanel() {
    JPanel panel = new JPanel(new FormLayout("pref,5dlu,pref,5dlu,pref,5dlu:grow,pref", "pref"));
    CellConstraints cc = new CellConstraints();

    {
      JPanel box = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 0));
      NumberFormat intFmt = NumberFormat.getIntegerInstance();
      intFmt.setGroupingUsed(false);
      JLabel label = BasicComponentFactory.createLabel(this.hitCount, intFmt);
      box.add(label);
      box.add(new JLabel("Treffer"));
      panel.add(box, cc.xy(1, 1));
    }

    panel.add(new JLabel("|"), cc.xy(2, 1));

    {
      JPanel box = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 0));
      NumberFormat intFmt = NumberFormat.getIntegerInstance();
      intFmt.setGroupingUsed(false);
      JLabel pageNum = BasicComponentFactory.createLabel(this.pageNum, intFmt);
      JLabel pageCount = BasicComponentFactory.createLabel(this.pageCount, intFmt);
      box.add(new JLabel("Seite"));
      box.add(pageNum);
      box.add(new JLabel("von"));
      box.add(pageCount);
      panel.add(box, cc.xy(3, 1));

      JToolBar toolbar = new JToolBar();
      toolbar.setFloatable(false);
      toolbar.add(new JButton(new SearchPrevious()));
      toolbar.add(new JButton(new SearchNext()));
      box.add(toolbar);

    }
    panel.add(new JLabel("|"), cc.xy(4, 1));
    
    {
      JPanel box = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
      
      ValueModel ownTracksModel = new ValueHolder(false);
      ownTracksModel.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          model.getModel("ownTracks").setValue(evt.getNewValue().equals(Boolean.TRUE) ? Boolean.TRUE : null);

        }
      });
      JCheckBox ownCb = BasicComponentFactory.createCheckBox(ownTracksModel, "eigene Titel");
      box.add(ownCb);

      ValueModel privateTracksModel = new ValueHolder(false);
      privateTracksModel.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          model.getModel("privateTracks").setValue(evt.getNewValue().equals(Boolean.TRUE) ? Boolean.TRUE : null);

        }
      });
      JCheckBox privateCb = BasicComponentFactory.createCheckBox(privateTracksModel, "private Titel");
      box.add(privateCb);
      panel.add(box, cc.xy(5, 1));
      
    }

    {
      Search searchAction = new Search();
      JToolBar toolbar = new JToolBar();
      toolbar.setFloatable(false);
      toolbar.add(new JButton(searchAction));
      panel.add(toolbar, cc.xy(7, 1));
      
      
    }

    searchResultHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        SearchResultSet resultSet = (SearchResultSet) evt.getNewValue();
        if (resultSet != null) {
          hitCount.setValue(resultSet.getTotalEntries());
          pageCount.setValue(resultSet.getTotalPages());
          pageNum.setValue(resultSet.getCurrentPage());
        } else {
          hitCount.setValue(0);
          pageCount.setValue(1);
          pageNum.setValue(1);
        }
      }
    });

    return panel;
  }

  JPanel createQueryPanel() {
    Search searchAction = new Search();

    JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    row1.add(createTextFieldQueryPanel("artist", searchAction, 15, false));
    row1.add(createTextFieldQueryPanel("title", searchAction, 15, false));
    row1.add(createTextFieldQueryPanel("album", searchAction, 15, false));
    row1.add(createTextFieldQueryPanel("year", searchAction, 4, true));

    JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

    ValueModel ownTracksModel = new ValueHolder(false);
    ownTracksModel.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        model.getModel("ownTracks").setValue(evt.getNewValue().equals(Boolean.TRUE) ? Boolean.TRUE : null);

      }
    });
    JCheckBox ownCb = BasicComponentFactory.createCheckBox(ownTracksModel, "eigene Titel");
    row2.add(ownCb);

    ValueModel privateTracksModel = new ValueHolder(false);
    privateTracksModel.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        model.getModel("privateTracks").setValue(evt.getNewValue().equals(Boolean.TRUE) ? Boolean.TRUE : null);

      }
    });
    JCheckBox privateCb = BasicComponentFactory.createCheckBox(privateTracksModel, "private Titel");
    row2.add(privateCb);

    JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
    searchBox.add(new JButton(searchAction));
    searchBox.add(new JLabel(" "));
    searchBox.add(new JButton(new SearchPrevious()));
    searchBox.add(new JButton(new SearchNext()));
    row2.add(searchBox);

    JPanel panel = new JPanel(new FormLayout("pref:grow", "3dlu,pref,5dlu,pref,3dlu"));
    panel.add(row1, new CellConstraints(1, 2));
    panel.add(row2, new CellConstraints(1, 4));

    return panel;
  }

  private JPanel createTextFieldQueryPanel(String property, Search searchAction, int cols, boolean number) {
    JPanel panel = new JPanel(new FormLayout("pref,3dlu,pref", "pref"));
    JLabel label = new JLabel(ctx.getTextProvider().getString("track." + property));
    panel.add(label, new CellConstraints(1, 1));

    NumberFormat fmt = NumberFormat.getIntegerInstance();
    fmt.setGroupingUsed(false);
    JTextField tf = number ? BasicComponentFactory.createIntegerField(this.model.getModel(property), fmt, 0) : BasicComponentFactory.createTextField(
        this.model.getModel(property), false);
    tf.setColumns(cols);
    tf.addActionListener(searchAction);
    panel.add(tf, new CellConstraints(3, 1));

    return panel;
  }

  abstract class BaseSearchAction extends AbstractAction {
    private static final long serialVersionUID = 7983662248044194876L;

    protected void doSearch() {
      try {
        searchResultHolder.setValue(ctx.getAdminClient().getTrackService().find(model.getBean()));
      } catch (Exception e) {
        JXErrorPane.showDialog(null, ctx.getTextProvider().createErrorInfo(e, "action.search.error"));
      }
    }

  }

  class Search extends BaseSearchAction {
    private static final long serialVersionUID = 4336873977190095444L;

    Search() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("searching.png"));
      this.putValue(Action.NAME, ctx.getTextProvider().getString("search"));
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      model.getBean().setPage(1);
      this.doSearch();
    }
  }

  class SearchNext extends BaseSearchAction implements PropertyChangeListener {
    private static final long serialVersionUID = 4336873977190095444L;

    SearchNext() {
      this.putValue(Action.NAME, " > ");
      this.setEnabled(false);
      this.putValue(Action.SHORT_DESCRIPTION, ctx.getTextProvider().getString("titlesearch.action.nextpage.tooltip"));
      searchResultHolder.addValueChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      model.getBean().setPage(model.getBean().getPage() + 1);
      this.doSearch();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      SearchResultSet resultSet = (SearchResultSet) evt.getNewValue();
      if (resultSet != null) {
        setEnabled(resultSet.getCurrentPage() < resultSet.getTotalPages());
      } else {
        setEnabled(false);
      }
    }
  }

  class SearchPrevious extends BaseSearchAction implements PropertyChangeListener {
    private static final long serialVersionUID = 4336873977190095444L;

    SearchPrevious() {
      this.putValue(Action.NAME, " < ");
      this.setEnabled(false);
      this.putValue(Action.SHORT_DESCRIPTION, ctx.getTextProvider().getString("titlesearch.action.previouspage.tooltip"));
      searchResultHolder.addValueChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      model.getBean().setPage(Math.max(1, model.getBean().getPage() - 1));
      this.doSearch();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      SearchResultSet resultSet = (SearchResultSet) evt.getNewValue();
      if (resultSet != null) {
        setEnabled(resultSet.getCurrentPage() > 1);
      } else {
        setEnabled(false);
      }
    }

  }

  /**
   * @return the searchResultHolder
   */
  public ValueModel getSearchResultHolder() {
    return searchResultHolder;
  }

  /**
   * @return the model
   */
  public PresentationModel<TrackQuery> getModel() {
    return model;
  }

  /**
   * @return the selectionHolder
   */
  public ValueModel getSelectionHolder() {
    return selectionHolder;
  }

}
