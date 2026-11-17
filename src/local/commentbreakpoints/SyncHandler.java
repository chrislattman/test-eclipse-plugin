package local.commentbreakpoints;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.notifications.NotificationPopup;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

/** "Comment Breakpoints: Set Breakpoints From Comments": syncs all open projects with one name. */
public class SyncHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) {
		Shell shell = HandlerUtil.getActiveShell(event);
		// The name can come from a key binding's parameter; otherwise ask, pre-filled with the setting
		String name = event.getParameter("name");
		if (name == null) {
			InputDialog dialog = new InputDialog(shell, "Comment Breakpoints",
					"Name in \"// <name> breakpoint N\" comments", Prefs.get(activeProject(event), Prefs.NAME), null);
			if (dialog.open() != Window.OK) {
				return null;
			}
			name = dialog.getValue();
		}
		if (name.isEmpty()) {
			return null;
		}
		Display display = shell != null ? shell.getDisplay() : Display.getDefault();
		List<IProject> projects = List.of(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		SyncJob.schedule(name.trim(), projects, summary -> display.asyncExec(() -> NotificationPopup
				.forDisplay(display).title("Comment Breakpoints", true).text(summary).open()));
		return null;
	}

	/** The project of the file in the active editor, whose settings pre-fill the prompt. */
	private static IProject activeProject(ExecutionEvent event) {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		IFile file = editor == null ? null : editor.getEditorInput().getAdapter(IFile.class);
		return file == null ? null : file.getProject();
	}
}
