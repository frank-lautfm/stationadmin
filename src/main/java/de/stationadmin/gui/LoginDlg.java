/**
 * 
 */
package de.stationadmin.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.TransferHandler;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.gui.playlist.PopupListener;
import de.stationadmin.gui.util.ClipboardAction;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;
import de.stationadmin.lfm.backend.LautfmAdminService;
import de.stationadmin.lfm.backend.Station;

/**
 * Login dialog
 * 
 * @author Frank Korf
 */
public class LoginDlg extends JDialog {
  private static final long serialVersionUID = 5295864819176416902L;
  public static final String ORIGIN = "StationAdmin";
  private LautfmAdminService adminService;
  private ClientContext ctx;
  private ValueHolder token = new ValueHolder();
  // private ValueHolder origin = new ValueHolder("StationAdmin");
  private ValueHolder stationList = new ValueHolder(new ArrayList<Station>(), true);
  private ValueHolder station = new ValueHolder();
  private ValueHolder remember = new ValueHolder(false);
  private ValueHolder messageText = new ValueHolder();
  private ValueHolder locale = new ValueHolder(Locale.GERMAN);

  public LoginDlg(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.loadDefaults();
    this.init();
  }

  private void resolveStations() {
    String token = (String) LoginDlg.this.token.getValue();
    if (StringUtils.isNotEmpty(token)) {
      if (this.adminService != null && this.adminService.getToken().equals(token)) {
        // no chanages
        return;
      }
      this.adminService = new LautfmAdminService(token, ORIGIN);
      try {
        List<Station> stations = adminService.getStations();
        Collections.sort(stations);
        this.stationList.setValue(stations);
        if (stations.size() > 0) {
          messageText.setValue(null);
        } else {
          messageText.setValue(ctx.getTextProvider().getString("action.login.msg.nostations"));

        }
      } catch (de.stationadmin.lfm.backend.AuthenticationException e) {
        Toolkit.getDefaultToolkit().beep();
        messageText.setValue(ctx.getTextProvider().getString("action.login.msg.autherror"));
      } catch (IOException e) {
        JXErrorPane.showDialog(null, ctx.getTextProvider().createErrorInfo(e, "login.action.error"));
      }

    }
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("10dlu,pref,5dlu,pref,2dlu,pref,10dlu", "10dlu,pref,5dlu,pref,0dlu,pref,5dlu,pref,5dlu,pref,10dlu,pref,10dlu"));
    CellConstraints cc = new CellConstraints();

    LoginAction loginAction = new LoginAction();

    Container panel = this.getContentPane();

    FocusAdapter focusListener = new FocusAdapter() {

      @Override
      public void focusLost(FocusEvent e) {
        resolveStations();
      }
    };

    // this.messageText.setValue("test");

    panel.add(new JLabel(ctx.getTextProvider().getString("login.property.token")), cc.xy(2, 2));
    JTextField tokenTf = BasicComponentFactory.createTextField(this.token, false);
    tokenTf.setColumns(20);
    tokenTf.addFocusListener(focusListener);
    tokenTf.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        resolveStations();
      }
    });
    tokenTf.setAction(loginAction);
    panel.add(tokenTf, cc.xy(4, 2));

    final JPopupMenu popup = new JPopupMenu();
    popup.add(new ClipboardAction(ctx, tokenTf, this.token, TransferHandler.getPasteAction()));
    tokenTf.addMouseListener(new PopupListener(tokenTf, popup));

    JButton requestTokenBtn = new JButton("?");
    requestTokenBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent evt) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        boolean browserRequest = false;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
          try {
            desktop.browse(new URI("https://radioadmin.laut.fm/login?callback_url=StationAdmin"));
            browserRequest = true;
          } catch (Exception e) {
          }
        }
        if (!browserRequest) {
          JDialog dlg = new JDialog();
          CellConstraints cc = new CellConstraints();
          dlg.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref,5dlu"));
          dlg.getContentPane().add(new JLabel("Bitte öffne die folgende URL in Deinem Browser:"), cc.xy(2, 2));
          JTextField tf = new JTextField(40);
          tf.setText("https://radioadmin.laut.fm/login?callback_url=StationAdmin");
          tf.setEditable(false);
          dlg.getContentPane().add(tf, cc.xy(2, 4));
          dlg.setSize(400, 100);
          dlg.setModal(true);
          SwingTools.centerOnScreen(dlg);

          dlg.setVisible(true);
        }
      }
    });

    panel.add(requestTokenBtn, cc.xy(6, 2));

    SelectionInList<Station> stationSelection = new SelectionInList<Station>(this.stationList, this.station);
    JComboBox<Station> stationCmb = (JComboBox<Station>) BasicComponentFactory.createComboBox(stationSelection);
    panel.add(new JLabel(ctx.getTextProvider().getString("login.property.station")), cc.xy(2, 6));
    panel.add(stationCmb, cc.xywh(4, 6, 3, 1));

    JPanel optionsPanel = new JPanel(new FormLayout("pref,5dlu:grow,pref", "pref"));

    JCheckBox rememberCb = BasicComponentFactory.createCheckBox(this.remember, ctx.getTextProvider().getString("login.remember"));
    optionsPanel.add(rememberCb, cc.xy(1, 1));

    SelectionInList<Locale> localeSelection = new SelectionInList<Locale>(new Locale[] { Locale.GERMAN, Locale.ENGLISH, Locale.ITALIAN }, this.locale);
    JComboBox localeCmb = BasicComponentFactory.createComboBox(localeSelection, new DefaultListCellRenderer() {

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Locale) {
          this.setText(((Locale) value).getLanguage().toUpperCase());
        }
        return this;
      }

    });
    optionsPanel.add(localeCmb, cc.xy(3, 1));

    panel.add(optionsPanel, cc.xywh(2, 8, 5, 1));

    JLabel msgLabel = BasicComponentFactory.createLabel(this.messageText);
    msgLabel.setFont(ComponentFactory.boldLabelFont);
    msgLabel.setForeground(Color.RED);
    panel.add(msgLabel, cc.xywh(2, 10, 3, 1));

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    buttonPanel.add(new JButton(loginAction));
    buttonPanel.add(new JButton(new DisposeAction(this, ctx.getTextProvider().getString("cancel"))));

    panel.add(buttonPanel, cc.xywh(2, 12, 5, 1, CellConstraints.CENTER, CellConstraints.CENTER));

    this.setTitle(ctx.getTextProvider().getString("login.title"));

    Dimension dim = this.getPreferredSize();
    this.setSize((int) dim.getWidth() + 20, (int) dim.getHeight() + 50);

    SwingTools.centerOnScreen(this);
  }

  @SuppressWarnings("unchecked")
  private void loadDefaults() {
    String token = Preferences.userRoot().get("token", null);
    // String origin = Preferences.userRoot().get("origin", null);
    if (token != null) {
      this.token.setValue(token);
      // this.origin.setValue(origin);
      this.remember.setValue(true);
      this.resolveStations();
      List<Station> stations = (List<Station>) stationList.getValue();
      int stationId = Preferences.userRoot().getInt("station", -1);
      if (stationId > -1 && stations != null && stations.size() > 0) {
        for (Station station : stations) {
          if (station.getId() == stationId) {
            this.station.setValue(station);
          }
        }
      }
    }
    String locale = Preferences.userRoot().get("locale", null);
    if (locale != null && locale.equalsIgnoreCase("en")) {
      this.locale.setValue(Locale.ENGLISH);
      ctx.getTextProvider().setLocale(Locale.ENGLISH);
    }
    if (locale != null && locale.equalsIgnoreCase("it")) {
      this.locale.setValue(Locale.ITALIAN);
      ctx.getTextProvider().setLocale(Locale.ITALIAN);
    }
  }

  private class LoginAction extends AbstractAction {
    private static final long serialVersionUID = -4860129426077664815L;

    LoginAction() {
      this.putValue(Action.NAME, ctx.getTextProvider().getString("login.action"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      if (adminService == null) {
        resolveStations();
      }

      String token = (String) LoginDlg.this.token.getValue();
      Station station = (Station) LoginDlg.this.station.getValue();
      if (adminService == null || station == null) {
        Toolkit.getDefaultToolkit().beep();
        return;
      }

      try {
        ctx.getTextProvider().setLocale((Locale) locale.getValue());
        StationAdminClient client = new StationAdminClient(adminService, station);
        ctx.setAdminClient(client);
        if (remember.getValue() instanceof Boolean && ((Boolean) remember.getValue()).booleanValue()) {
          Preferences.userRoot().put("token", token);
          Preferences.userRoot().put("origin", ORIGIN);
          Preferences.userRoot().putInt("station", station.getId());
        } else {
          Preferences.userRoot().remove("token");
          Preferences.userRoot().remove("origin");
          Preferences.userRoot().remove("station");
        }
        Preferences.userRoot().put("locale", ((Locale) locale.getValue()).getLanguage());
        dispose();
      } catch (Exception ex) {
        JXErrorPane.showDialog(null, ctx.getTextProvider().createErrorInfo(ex, "login.action.error"));
      }

    }

  }

}
