package local.commentbreakpoints;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;

/** On startup, syncs every open project with that project's configured name. */
public class Startup implements IStartup {
	@Override
	public void earlyStartup() {
		Map<String, List<IProject>> byName = new LinkedHashMap<>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (project.isOpen()) {
				byName.computeIfAbsent(Prefs.get(project, Prefs.NAME), k -> new ArrayList<>()).add(project);
			}
		}
		byName.forEach((name, projects) -> SyncJob.schedule(name, projects, null));
	}
}
