package de.stationadmin.gui.tag;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tag.Tag;
import de.stationadmin.base.util.TagRenameCommand;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.ThreadedAction;

public class RenameTagDlg extends JDialog {
	private static final long serialVersionUID = -8056444351476400704L;
	private ClientContext ctx;
	private Tag tag;
  private ValueHolder tagName = new ValueHolder();
	
	public RenameTagDlg(ClientContext ctx, Tag tag) {
		this.ctx = ctx;
		this.tag = tag;
		this.tagName.setValue(tag.getName());
		this.setTitle("Tag umbenenen");
		this.init();
	}
	
	private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref,5dlu"));

    JTextField nameTf = BasicComponentFactory.createTextField(this.tagName);
    this.getContentPane().add(nameTf, new CellConstraints(2,2,CellConstraints.FILL, CellConstraints.FILL));
        
    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    JButton okBtn = new JButton(new OkAction());

    buttonPanel.add(okBtn);
    buttonPanel.add(new JButton(new DisposeAction(this, "Abbruch"))); // FIXME
    // localize
    this.getContentPane().add(buttonPanel, new CellConstraints(2, 4, CellConstraints.CENTER, CellConstraints.CENTER));

    this.pack();
    int w = getSize().width;
    int h = getSize().height;
    this.setSize(w + 100, h);
		
	}
	
	class OkAction extends ThreadedAction {
		private static final long serialVersionUID = 1861670352698596467L;
		private TagRenameCommand cmd;
		
		public OkAction() {
			this.putValue(Action.NAME, "Ok");
			this.cmd = new TagRenameCommand(ctx.getAdminClient(), tag);
		}

		@Override
		protected boolean beforeExecution() {
      String name = tagName.getString();
      
      if (name.length() == 0) {
        ErrorInfo errorInfo = ctx.getTextProvider().createErrorInfo(null, "titletagmanager.action.save.illegalname.empty");
        JXErrorPane.showDialog(null, errorInfo);
      	return false;
      } else if (name.contains("/")) {
        ErrorInfo errorInfo = ctx.getTextProvider().createErrorInfo(null, "titletagmanager.action.save.illegalname.slash");
        JXErrorPane.showDialog(null, errorInfo);
      	return false;
      } else {
      	return true;
      }
		}

		@Override
		protected void onSuccess() {
			dispose();
		}

		@Override
		protected String getStatus() {
			return cmd.getStatus();
		}

		@Override
		protected void performAction() throws Exception {
			cmd.execute(tagName.getString());
		}

		@Override
		protected void showError(Exception e) {
      ErrorInfo errorInfo = ctx.getTextProvider().createErrorInfo(e, "titletagmanager.action.rename.failed");
      JXErrorPane.showDialog(null, errorInfo);			
		}
		
	}
	
}
