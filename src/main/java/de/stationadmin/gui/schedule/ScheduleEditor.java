/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.base.schedule.Schedule.Entry;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.PlaylistListCellRenderer;
import de.stationadmin.gui.playlist.PlaylistSelector;

/**
 * @author korf
 * 
 */
public class ScheduleEditor extends JPanel {
	private static final long serialVersionUID = 7506462225302959600L;
	private ClientContext ctx;
	private TextProvider textProvider;
	private ScheduleTableModel model;
	private int preferredTableHeight;
	private ValueModel selectionHolder = new ValueHolder();
	private PlaylistSelector playlistSelector;

	/**
	 * @param ctx
	 * @throws HeadlessException
	 */
	public ScheduleEditor(ClientContext ctx) {
		super();
		this.ctx = ctx;
		this.textProvider = ctx.getTextProvider();
		this.model = new ScheduleTableModel(ctx.getTextProvider(),
				ctx.getAdminClient().getPlaylistService().getPlaylistRegistry(), ctx.getAdminClient().getSchedule());
		this.init();
	}

	private void init() {
		JToolBar toolbar = new JToolBar();
		toolbar.add(new SaveAction());
		toolbar.add(new ResetAction());
		toolbar.addSeparator();
		toolbar.add(new EntryDeleteAction());
		toolbar.add(new ClearAction());
		toolbar.addSeparator();
		toolbar.add(new ImportAction());
		toolbar.add(new ExportAction());
		toolbar.addSeparator();
		toolbar.add(new ShuffleDlgAction());
		toolbar.add(new ExportHTMLAction());

		ValueModel selectionHolder = new ValueHolder();
		playlistSelector = new PlaylistSelector(ctx, PlaylistType.ONLINE, selectionHolder);
		playlistSelector.setListCellRenderer(new ScheduledPlaylistListCellRenderer());
		// selector.setListCellRenderer(new SimplePlaylistListCellRender());
		playlistSelector.setTransferHandler(new PlaylistSelectorTransferHandler(selectionHolder));

		JComponent table = this.createScheduleTable();

		this.setLayout(new FormLayout("5dlu,pref,5dlu,100dlu:grow,5dlu",
				"2dlu,pref,2dlu," + (this.preferredTableHeight + 30) + "px,5dlu:grow"));
		CellConstraints cc = new CellConstraints();

		this.add(toolbar, cc.xywh(2, 2, 4, 1));
		this.add(playlistSelector, cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));
		this.add(table, cc.xy(4, 4, CellConstraints.FILL, CellConstraints.FILL));
	}

	private JComponent createScheduleTable() {
		final JXTable table = new JXTable(this.model);
		table.setCellSelectionEnabled(true);
		table.getColumnModel().getColumn(0).setMaxWidth(40);
		table.setTransferHandler(new ScheduleTableTransferHandler(table));

		table.addHighlighter(new AbstractHighlighter() {

			@Override
			protected Component doHighlight(Component comp, ComponentAdapter adapter) {
				int row = table.convertRowIndexToModel(adapter.row);
				if (adapter.column == 0) {
					comp.setBackground(Color.LIGHT_GRAY);
				} else {
					if (!adapter.isSelected()) {
						ScheduleTableEntry entry = model.getEntryAt(adapter.column - 1, row);
						if (entry != null) {
							comp.setBackground(entry.getColor());
							comp.setForeground(entry.getFontColor());
						}
					}
				}
				return comp;
			}

		});

		ListSelectionListener selectionListener = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int row = table.getSelectedRow();
					int col = table.getSelectedColumn();
					if (row > -1 && col > 0) {
						selectionHolder.setValue(model.getEntryAt(col - 1, row));
					} else {
						selectionHolder.setValue(null);
					}
				}

			}
		};

		table.getSelectionModel().addListSelectionListener(selectionListener);
		table.getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);

		table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		table.getActionMap().put("delete", new EntryDeleteAction());
		this.preferredTableHeight = (int) table.getPreferredSize().getHeight(); // not
		// a
		// nice
		// way
		// to
		// return
		// this
		// information

		return new JScrollPane(table);
	}

	private class EntryDeleteAction extends AbstractAction {
		private static final long serialVersionUID = 6062294811028160866L;

		/**
		 * @param table
		 */
		public EntryDeleteAction() {
			super();
			this.putValue(Action.SMALL_ICON, ctx.getIcon("delete.png"));
			this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("scheduleeditor.action.delete.tooltip"));
			selectionHolder.addValueChangeListener(new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					setEnabled(evt.getNewValue() != null);
				}

			});
			this.setEnabled(false);
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			ScheduleTableEntry entry = (ScheduleTableEntry) selectionHolder.getValue();
			if (entry != null) {
				if (entry.getHour() == 0) {
					model.setPlaylistAt(ctx.getAdminClient().getSchedule().getBasePlaylist(), entry.getWeekday().ordinal(), 0);
				} else {
					model.removeEntry(entry);
				}
				try {
					playlistSelector.repaint();
				} catch (Exception ex) {
				}
			} else {
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}

	private class ScheduleTableTransferHandler extends TransferHandler {
		private static final long serialVersionUID = 191262405701623126L;
		private JXTable table;

		/**
		 * @param table
		 */
		public ScheduleTableTransferHandler(JXTable table) {
			super();
			this.table = table;
		}

		/**
		 * @see javax.swing.TransferHandler#canImport(javax.swing.TransferHandler.TransferSupport)
		 */
		@Override
		public boolean canImport(TransferSupport support) {
			return support.isDataFlavorSupported(DataFlavor.stringFlavor);
		}

		/**
		 * @see javax.swing.TransferHandler#importData(javax.swing.TransferHandler.TransferSupport)
		 */
		@Override
		public boolean importData(TransferSupport support) {
			// if we can't handle the import, say so
			if (!canImport(support)) {
				return false;
			}

			int row = 0;
			int col = 0;
			if (support.isDrop()) {
				// fetch the drop location
				JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
				row = dl.getRow();
				col = dl.getColumn();
			} else {
				row = table.getSelectedRow();
				col = table.getSelectedColumn();
			}

			if (row < 0 || col < 1) {
				return false;
			}

			try {
				for (DataFlavor flavor : support.getDataFlavors()) {
					if (flavor.equals(DataFlavor.stringFlavor)) {
						String string = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
						for (Playlist playlist : ctx.getAdminClient().getPlaylistService().getPlaylistRegistry()
								.getPlaylists(PlaylistType.ONLINE)) {
							if (playlist.getName().equals(string)) {
								((ScheduleTableModel) table.getModel()).setPlaylistAt(playlist, col - 1, row);
								try {
									playlistSelector.repaint();
								} catch (Exception e) {

								}
								break;
							}
						}
						break;
					}
				}
			} catch (UnsupportedFlavorException e) {
				return false;
			} catch (IOException e) {
				return false;
			}

			return false;

		}

	}

	private class ScheduledPlaylistListCellRenderer extends PlaylistListCellRenderer {
		private static final long serialVersionUID = -3517643561344614419L;

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof Playlist) {
				Playlist playlist = (Playlist) value;

				int cnt = 0;
				try {
					for (Entry entry : model.getEntries()) {
						if (entry.getPlaylistId() == playlist.getId())
							cnt++;
					}
				} catch (Exception e) {
				}
				this.setText(playlist.getDisplayName() + " (" + cnt + ")");
				if (this.renderColors && playlist.getType() == PlaylistType.ONLINE) {
					this.setIcon(this.getColorIcon(playlist.getColor()));
				}
			}
			return comp;
		}

	}

	private static class PlaylistSelectorTransferHandler extends TransferHandler {
		private static final long serialVersionUID = -1001077988161481329L;
		private ValueModel selectionHolder;

		/**
		 * @param listModel
		 */
		public PlaylistSelectorTransferHandler(ValueModel selectionHolder) {
			super();
			this.selectionHolder = selectionHolder;
		}

		/**
		 * @see javax.swing.TransferHandler#createTransferable(javax.swing.JComponent)
		 */
		@Override
		protected Transferable createTransferable(JComponent c) {
			if (selectionHolder.getValue() instanceof Playlist) {
				return new StringSelection(((Playlist) selectionHolder.getValue()).getName());
			} else {
				return null;
			}
		}

		/**
		 * @see javax.swing.TransferHandler#getSourceActions(javax.swing.JComponent)
		 */
		@Override
		public int getSourceActions(JComponent c) {
			return TransferHandler.COPY;
		}

		/**
		 * @see javax.swing.TransferHandler#exportToClipboard(javax.swing.JComponent,
		 *      java.awt.datatransfer.Clipboard, int)
		 */
		@Override
		public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
			String string = selectionHolder.getValue() != null ? ((Playlist) selectionHolder.getValue()).getName() : null;
			if (string != null) {
				StringSelection stringSelection = new StringSelection(string);
				clip.setContents(stringSelection, stringSelection);
			}
		}

	}

	private class SaveAction extends AbstractAction implements PropertyChangeListener {
		private static final long serialVersionUID = 5196031665526092020L;

		SaveAction() {
			this.putValue(Action.SMALL_ICON, ctx.getIcon("save.png"));
			this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("scheduleeditor.action.save.tooltip"));
			this.setEnabled(false);
			model.getModified().addValueChangeListener(this);
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent evt) {
			model.commit();
			try {
				ctx.getAdminClient().getSchedule().submitToServer();
				ctx.getAdminClient().getSchedule().save();
			} catch (Exception e) {
				JXErrorPane.showDialog(ctx.getRootWindow(), textProvider.createErrorInfo(e, "scheduleeditor.msg.save.failed"));
			}
		}

		/**
		 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
		 */
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			this.setEnabled((Boolean) model.getModified().getValue());
		}

	}

	private class ImportAction extends AbstractAction {
		private static final long serialVersionUID = -2397319819241534818L;

		ImportAction() {
			this.putValue(Action.SMALL_ICON, ctx.getIcon("schedule_import.png"));
			this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("scheduleeditor.action.import.tooltip"));
			this.setEnabled(true);
		}

		public void actionPerformed(ActionEvent evt) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileNameExtensionFilter("sched - Sendeplan", "sched"));
			if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				try {
					String filename = fileChooser.getSelectedFile().getAbsolutePath();
					List<Entry> entries = ctx.getAdminClient().getSchedule().loadEntries(filename);
					model.setEntries(filter(entries));
				} catch (Exception e) {
					JXErrorPane.showDialog(ctx.getRootWindow(),
							textProvider.createErrorInfo(e, "scheduleeditor.msg.save.failed"));
				}

			}
		}

		private List<Entry> filter(List<Entry> entries) {
			List<Entry> filtered = new ArrayList<Entry>();
			for (Schedule.Entry entry : entries) {
				if (entry.getHour() > -1) {
					filtered.add(entry);
				}
				// else: contains information about base playlist - can't restore here
			}
			return filtered;

		}

	}

	private class ExportAction extends AbstractAction implements PropertyChangeListener {
		private static final long serialVersionUID = -3147136069408559324L;

		ExportAction() {
			this.putValue(Action.SMALL_ICON, ctx.getIcon("playlist_export.png"));
			this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("scheduleeditor.action.export.tooltip"));
			this.setEnabled(true);
			model.getModified().addValueChangeListener(this);
		}

		/**
		 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
		 */
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			this.setEnabled(!(Boolean) model.getModified().getValue());
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent evt) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileNameExtensionFilter("sched - Sendeplan", "sched"));
			if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
				try {
					String filename = fileChooser.getSelectedFile().getAbsolutePath();
					if (!filename.toLowerCase().endsWith(".sched")) {
						filename += ".sched";
					}
					ctx.getAdminClient().getSchedule().submitToServer();
					ctx.getAdminClient().getSchedule().save(filename);
				} catch (Exception e) {
					JXErrorPane.showDialog(ctx.getRootWindow(),
							textProvider.createErrorInfo(e, "scheduleeditor.msg.save.failed"));
				}

			}
		}

	}

	private class ResetAction extends AbstractAction implements PropertyChangeListener {
		private static final long serialVersionUID = -2112027836623659834L;

		ResetAction() {
			this.putValue(Action.SMALL_ICON, ctx.getIcon("undo.png"));
			this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("scheduleeditor.action.reset.tooltip"));
			this.setEnabled(false);
			model.getModified().addValueChangeListener(this);
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			model.reset();
		}

		/**
		 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
		 */
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			this.setEnabled((Boolean) model.getModified().getValue());
		}

	}

	private class ClearAction extends AbstractAction {
		private static final long serialVersionUID = -3896217338903773513L;

		ClearAction() {
			this.putValue(Action.SMALL_ICON, ctx.getIcon("trash.png"));
			this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("scheduleeditor.action.clear.tooltip"));
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			model.clear();
		}

	}

	private class ExportHTMLAction extends AbstractAction {
		private static final long serialVersionUID = -3967083263374255469L;

		ExportHTMLAction() {
			this.putValue(Action.SMALL_ICON, ctx.getIcon("html.png"));
			this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("scheduleeditor.action.html.tooltip"));
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			HTMLExportDlg dlg = new HTMLExportDlg(ctx, model);
			dlg.setVisible(true);
		}
	}

	private class ShuffleDlgAction extends AbstractAction {
		private static final long serialVersionUID = 2594800254185182408L;

		ShuffleDlgAction() {
			this.putValue(Action.SMALL_ICON, ctx.getIcon("shuffle.png"));
			this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("scheduleeditor.action.shuffle.tooltip"));
		}

		public void actionPerformed(ActionEvent e) {
			ShuffleDlg dlg = new ShuffleDlg(ctx, model);
			dlg.setVisible(true);
		}
	}

	/**
	 * @return the model
	 */
	ScheduleTableModel getModel() {
		return model;
	}

}
