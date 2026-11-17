package local.commentbreakpoints;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Settings, looked up per project first (.settings/local.commentbreakpoints.prefs, the
 * counterpart of .vscode/settings.json), then workspace-wide, then the defaults below.
 */
public class Prefs extends AbstractPreferenceInitializer {
	public static final String QUALIFIER = "local.commentbreakpoints";
	public static final String NAME = "name";
	public static final String INCLUDE = "include";
	public static final String EXCLUDE = "exclude";

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences d = DefaultScope.INSTANCE.getNode(QUALIFIER);
		d.put(NAME, "chris");
		d.put(INCLUDE, "**/*.{c,h,cpp,hpp,cc,S,rs}");
		d.put(EXCLUDE, "**/{build,bsp,*_bsp,target,.git}/**");
	}

	/** Looks a setting up for a project, or workspace-wide when project is null. */
	public static String get(IProject project, String key) {
		IScopeContext[] scopes = project == null
				? new IScopeContext[] { InstanceScope.INSTANCE }
				: new IScopeContext[] { new ProjectScope(project), InstanceScope.INSTANCE };
		return Platform.getPreferencesService().getString(QUALIFIER, key, "", scopes);
	}
}
