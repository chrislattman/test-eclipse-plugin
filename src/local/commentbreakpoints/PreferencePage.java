package local.commentbreakpoints;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Edits the settings workspace-wide (as a preference page) or for one project (as a
 * property page, stored in the project's .settings folder).
 */
public class PreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
	private IAdaptable element;

	public PreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public IAdaptable getElement() {
		return element;
	}

	@Override
	public void setElement(IAdaptable element) {
		this.element = element;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		IProject project = element == null ? null : element.getAdapter(IProject.class);
		if (project == null) {
			return new ScopedPreferenceStore(InstanceScope.INSTANCE, Prefs.QUALIFIER);
		}
		ProjectScope scope = new ProjectScope(project);
		ScopedPreferenceStore store = new ScopedPreferenceStore(scope, Prefs.QUALIFIER);
		store.setSearchContexts(new IScopeContext[] { scope, InstanceScope.INSTANCE });
		return store;
	}

	@Override
	protected void createFieldEditors() {
		setDescription(element == null
				? "Settings for all projects; a project's Properties > Comment Breakpoints page overrides them."
				: "Settings for this project only.");
		addField(new StringFieldEditor(Prefs.NAME, "Name in \"// <name> breakpoint N\":", getFieldEditorParent()));
		addField(new StringFieldEditor(Prefs.INCLUDE, "Include (file glob):", getFieldEditorParent()));
		addField(new StringFieldEditor(Prefs.EXCLUDE, "Exclude (file glob):", getFieldEditorParent()));
	}

	@Override
	public boolean performOk() {
		boolean ok = super.performOk();
		try {
			((ScopedPreferenceStore) getPreferenceStore()).save();
		} catch (IOException e) {
			setErrorMessage("Couldn't save: " + e.getMessage());
			return false;
		}
		return ok;
	}
}
