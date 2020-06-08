package de.stationadmin.gui.playlist;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.util.PlaylistConfigExplain;
import de.stationadmin.base.playlist.util.PlaylistConfigExplain.ConfigItem;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminDialog;
import de.stationadmin.gui.util.ComponentFactory;

public class PlaylistConfigExplainDlg extends StationAdminDialog {
  private static final long serialVersionUID = -4359297505556223280L;
  PlaylistConfigExplain explain;

  public PlaylistConfigExplainDlg(ClientContext ctx, Playlist playlist) throws HeadlessException {
    super(ctx, "CfgExplain");
    explain = new PlaylistConfigExplain(ctx.getAdminClient().getPlaylistService(), ctx.getAdminClient().getTrackService().getTrackRegistry());
    this.initialize(playlist);
  }

  private void initialize(Playlist playlist) {
    this.setTitle("Konfiguration - " + playlist.getName());

    StyleContext sc = new StyleContext();
    DefaultStyledDocument doc = new DefaultStyledDocument(sc);
    JTextPane pane = new JTextPane(doc);
    pane.setEditable(false);

    Font font = ComponentFactory.defaultLabelFont;

    final Style style = sc.addStyle("Standard", null);
    style.addAttribute(StyleConstants.FontSize, font.getSize() + 2);
    style.addAttribute(StyleConstants.FontFamily, font.getFamily());
    style.addAttribute(StyleConstants.SpaceBelow, 4f);

    List<ConfigItem> items = explain.explain(playlist);
    int offset = 0;
    for (ConfigItem item : items) {
      String text = ctx.getTextProvider().getString("playlistcfg.explain." + item.getValueKey(), item.getValueOpts()) + "\n";

      try {
        doc.insertString(offset, text, null);
        offset += text.length();
      } catch (BadLocationException e) {

      }
    }
    doc.setParagraphAttributes(0, offset, style, true);


    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(new JScrollPane(pane), BorderLayout.CENTER);
  }

  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(650, 300);
  }

}
