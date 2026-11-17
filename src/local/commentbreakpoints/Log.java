package local.commentbreakpoints;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/** The "Comment Breakpoints" console (Console view > Open Console), like VS Code's output channel. */
final class Log {
	private static MessageConsoleStream stream;

	private Log() {
	}

	static synchronized void appendLine(String line) {
		if (stream == null) {
			IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
			MessageConsole console = new MessageConsole("Comment Breakpoints", null);
			manager.addConsoles(new IConsole[] { console });
			stream = console.newMessageStream();
		}
		stream.println(line);
	}
}
