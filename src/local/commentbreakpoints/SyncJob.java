package local.commentbreakpoints;

import java.util.Collection;
import java.util.function.Consumer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/** Runs a sync in the background; syncs never overlap, so a line can't get two breakpoints. */
final class SyncJob extends WorkspaceJob {
	private final String name;
	private final Collection<IProject> projects;
	private final Consumer<String> onDone;

	private SyncJob(String name, Collection<IProject> projects, Consumer<String> onDone) {
		super("Comment Breakpoints: " + name);
		this.name = name;
		this.projects = projects;
		this.onDone = onDone;
		setRule(ResourcesPlugin.getWorkspace().getRoot());
	}

	/** Schedules a sync; onDone, if given, receives the summary line. */
	static void schedule(String name, Collection<IProject> projects, Consumer<String> onDone) {
		new SyncJob(name, projects, onDone).schedule();
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {
		try {
			String summary = Sync.sync(name, projects);
			if (onDone != null) {
				onDone.accept(summary);
			}
			return Status.OK_STATUS;
		} catch (CoreException | RuntimeException e) {
			Log.appendLine("\"" + name + "\": failed: " + e.getMessage());
			return Status.error("Comment Breakpoints failed for \"" + name + "\"", e);
		}
	}
}
