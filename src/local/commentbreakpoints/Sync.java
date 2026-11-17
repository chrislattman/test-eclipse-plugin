package local.commentbreakpoints;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.cdt.debug.core.CDIDebugModel;
import org.eclipse.cdt.debug.core.model.ICBreakpoint;
import org.eclipse.cdt.debug.core.model.ICBreakpointType;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;

/** Scans the given projects and adds a C/C++ line breakpoint after each marker comment. */
final class Sync {
	// JavaScript's \s, so whitespace matches exactly as in the VS Code extension
	private static final String WS = "[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]";

	private Sync() {
	}

	/** Matches "// <name> breakpoint N"; the name is quoted so it's matched literally. */
	static Pattern markerFor(String name) {
		return Pattern.compile("//" + WS + "*" + Pattern.quote(name) + WS + "+breakpoint" + WS + "*[0-9]+");
	}

	private record Added(IFile file, String sourceHandle, int line) {
	}

	/** Runs the sync for one name over the given projects and returns the summary line. */
	static String sync(String name, Collection<IProject> projects) throws CoreException {
		Pattern marker = markerFor(name);
		List<IFile> files = findFiles(projects);

		// Skip lines that already have a line breakpoint so re-running is harmless
		Set<String> existing = new HashSet<>();
		for (IBreakpoint bp : DebugPlugin.getDefault().getBreakpointManager().getBreakpoints()) {
			if (!(bp instanceof ILineBreakpoint lineBp)) {
				continue;
			}
			try {
				String handle = bp instanceof ICBreakpoint cBp ? cBp.getSourceHandle()
						: sourceHandle(bp.getMarker().getResource());
				existing.add(key(handle, lineBp.getLineNumber()));
			} catch (CoreException | RuntimeException e) {
				// Breakpoint whose marker is gone or unreadable: nothing to compare against
			}
		}

		List<Added> added = new ArrayList<>();
		int alreadySet = 0;
		for (IFile file : files) {
			String[] lines = new String(file.readAllBytes(), StandardCharsets.UTF_8).split("\r?\n", -1);
			String handle = sourceHandle(file);
			for (int i = 0; i < lines.length; i++) {
				if (!marker.matcher(lines[i]).find() || i + 1 >= lines.length) {
					continue;
				}
				int line = i + 2; // one-based: the line after the comment
				if (existing.contains(key(handle, line))) {
					alreadySet++;
					continue;
				}
				added.add(new Added(file, handle, line));
			}
		}
		if (!added.isEmpty()) {
			ResourcesPlugin.getWorkspace().run(monitor -> {
				for (Added a : added) {
					CDIDebugModel.createLineBreakpoint(a.sourceHandle(), a.file(), ICBreakpointType.REGULAR,
							a.line(), true, 0, "", true);
				}
			}, null);
		}
		for (Added a : added) {
			Log.appendLine("added " + a.file().getFullPath().makeRelative() + ":" + a.line());
		}
		String summary = "\"" + name + "\": " + added.size() + " breakpoint(s) added, " + alreadySet + " already set";
		Log.appendLine(summary + ", " + files.size() + " file(s) scanned");
		return summary;
	}

	/** Files matching each project's include glob and not its exclude glob. */
	private static List<IFile> findFiles(Collection<IProject> projects) throws CoreException {
		List<IFile> files = new ArrayList<>();
		Set<String> seen = new HashSet<>(); // nested projects can expose the same file twice
		for (IProject project : projects) {
			if (!project.isAccessible()) {
				continue;
			}
			PathMatcher include = matcher(Prefs.get(project, Prefs.INCLUDE));
			String excludeGlob = Prefs.get(project, Prefs.EXCLUDE);
			PathMatcher exclude = excludeGlob.isEmpty() ? null : matcher(excludeGlob);
			project.accept(proxy -> {
				if (proxy.getType() != IResource.FILE) {
					return true;
				}
				String rel = proxy.requestFullPath().removeFirstSegments(1).toString();
				if (matches(include, rel) && (exclude == null || !matches(exclude, rel))) {
					IFile file = (IFile) proxy.requestResource();
					if (seen.add(sourceHandle(file))) {
						files.add(file);
					}
				}
				return true;
			}, IResource.NONE);
		}
		return files;
	}

	private static PathMatcher matcher(String glob) {
		return FileSystems.getDefault().getPathMatcher("glob:" + glob);
	}

	// In VS Code globs "**/" also matches no folders at all ("**/*.c" matches "main.c");
	// Java's needs a slash before the name, so try the path with a leading one as well.
	private static boolean matches(PathMatcher m, String rel) {
		return m.matches(Path.of(rel)) || m.matches(Path.of("/" + rel));
	}

	// CDT identifies a breakpoint's file by its absolute OS path (the "source handle")
	private static String sourceHandle(IResource resource) {
		IPath location = resource.getLocation();
		return location != null ? location.toOSString() : resource.getFullPath().toString();
	}

	private static String key(String handle, int line) {
		String normalized = handle;
		try {
			normalized = Path.of(handle).toAbsolutePath().normalize().toString();
		} catch (RuntimeException e) {
			// Not a valid OS path; compare it as is
		}
		return normalized + ":" + line;
	}
}
