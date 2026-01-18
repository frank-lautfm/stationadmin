/**
 * 
 */
package de.stationadmin.gui.player;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.playlist.PopupListener;
import de.stationadmin.gui.track.CopyTracksAction;
import de.stationadmin.gui.track.DistributeTracksAction;
import de.stationadmin.gui.track.TagMenu;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

/**
 * @author korf
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SnippetPlayer extends StationAdminFrame {
  private static final long serialVersionUID = -8772607010513250934L;

  private PlayerThread playerThread;
  private ValueHolder time = new ValueHolder(0);
  private ValueHolder source = new ValueHolder();
  private JList list;
  private Timer timeRefresher;
  private volatile State state = State.STARTING;
  CloseableHttpClient client;

  /**
   * @param ctx
   * @throws HeadlessException
   */
  public SnippetPlayer(ClientContext ctx, List<Snippet> snippets) throws HeadlessException {
    super(ctx, "snippetplayer");
    this.client = ctx.getAdminClient().getSessionCtx().getServer().createClient();
    this.init(snippets);
  }

  private void init(List<Snippet> snippets) {
    this.setTitle(ctx.getString("snippetplayer.title"));

    this.getContentPane().setLayout(new FormLayout("5dlu,100dlu:grow,5dlu", "5dlu,10dlu:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    final TagMenu tagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), true);
    final TagMenu untagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), false);
    final CopyTracksAction copyAction = new CopyTracksAction(this.ctx);
    final DistributeTracksAction distributeAction = new DistributeTracksAction(this.ctx);

    ListModel listModel = new IndirectListModel<Snippet>(snippets);
    this.list = new JList(listModel);
    this.list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.list.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          int idx = list.getSelectedIndex();
          int[] titleIds = null;
          List<BasicTrack> titles = new ArrayList<BasicTrack>();
          BasicTrack title = null;
          if (idx > -1) {
            Snippet snippet = (Snippet) list.getModel().getElementAt(idx);
            play(snippet);
            title = snippet.getTitle();
            titleIds = new int[] { title.getId() };
            titles.add(title);
          } else {
            stop();
          }
          tagMenu.setTitleIds(titleIds);
          untagMenu.setTitleIds(titleIds);
          copyAction.setTitles(titles);
          distributeAction.setTitles(titles);
        }

      }
    });

    this.getContentPane().add(new JScrollPane(list), cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    JPanel ctrlPanel = new JPanel(new FormLayout("pref,7dlu,pref,2dlu,pref,5dlu:grow,pref", "pref"));

    final JLabel label = new JLabel(TimeFormat.format(0, false));
    this.time.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        label.setText(TimeFormat.format((Integer) evt.getNewValue(), false));
      }
    });

    ctrlPanel.add(label, cc.xy(1, 1));

    ctrlPanel.add(new JLabel(ctx.getString("snippetplayer.property.source") + ":"), cc.xy(3, 1));
    ctrlPanel.add(BasicComponentFactory.createLabel(this.source), cc.xy(5, 1));

    JToolBar tb = new JToolBar();
    tb.setFloatable(false);
    JButton stop = new JButton(ctx.getIcon("player_stop.png"));
    stop.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        state = State.STOPPED;
        list.clearSelection();
      }
    });
    tb.add(stop);
    ctrlPanel.add(tb, cc.xy(7, 1));

    final JPopupMenu popup = new JPopupMenu();
    popup.add(tagMenu);
    popup.add(untagMenu);
    popup.addSeparator();
    popup.add(copyAction);
    popup.add(distributeAction);

    list.addMouseListener(new PopupListener(list, popup));

    this.getContentPane().add(ctrlPanel, cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    this.addWindowListener(new WindowAdapter() {

      @Override
      public void windowClosing(WindowEvent e) {
        stop();
      }
    });
  }

  private void stop() {
    if (this.timeRefresher != null) {
      this.timeRefresher.stop();
      this.timeRefresher = null;
    }
    if (this.playerThread != null) {
      this.playerThread.close();
      this.playerThread = null;
      this.time.setValue(0);
    }
    this.source.setValue(null);
  }

  private void playNext() {
    if (state == State.STARTING) {
      list.setSelectedIndex(0);
    } else if (state != State.STOPPED) {
      int idx = list.getSelectedIndex();
      int next = idx < 0 ? list.getModel().getSize() : idx + 1;
      if (next < list.getModel().getSize()) {
        list.setSelectedIndex(next);
      } else {
        list.clearSelection();
      }
    } else {
      list.clearSelection();
    }
  }

  private void play(Snippet snippet) {
    this.stop();
    try {
      this.state = State.PLAYING;
      this.source.setValue(snippet.getSource());
      this.playerThread = new PlayerThread(snippet);
      this.playerThread.start();

      this.timeRefresher = new Timer(500, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          time.setValue(playerThread != null ? playerThread.player.getPosition() / 1000 : 0);

        }
      });
      this.timeRefresher.start();

    } catch (Exception e) {
      LogManager.getLogger(SnippetPlayer.class).error("Play of " + snippet + " failed", e);
      displayErrorInEDT(e);
    }
  }

  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(400, 300);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      this.list.setSelectedIndex(0);
    }

  }

  void displayErrorInEDT(final Exception e) {
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        ErrorInfo info = ctx.createErrorInfo(e, "action.playsnippet.msg.error");
        JXErrorPane.showDialog(ctx.getRootWindow(), info);
      }
    });
  }

  enum State {
    STARTING, PLAYING, STOPPED;

  }

  class PlayerThread extends Thread {
    private volatile Player player;
    private volatile Snippet snippet;
    private volatile boolean stopped = false;
    private CloseableHttpResponse response;

    PlayerThread(Snippet snippet) throws IOException, JavaLayerException {
      this.snippet = snippet;

      HttpGet request = new HttpGet(snippet.getUrl().toString());
      response = client.execute(request);
      this.player = new Player(response.getEntity().getContent());
    }

    public void run() {
      try {
        this.player.play();
      } catch (Exception e) {
        displayErrorInEDT(e);
        LogManager.getLogger(SnippetPlayer.class).error("Play of " + snippet + " failed", e);
      } finally {
        try {
          response.close();
        } catch (Exception e) {
        }
      }
      if (!stopped) {
        playNext();
      }

    }

    void close() {
      this.stopped = true;
      this.player.close();
    }
  }

}
