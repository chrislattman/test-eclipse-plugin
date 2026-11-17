# Comment Breakpoints (Eclipse)

An Eclipse CDT plugin that sets a breakpoint on the line right after every
`// <name> breakpoint N` comment, for example `// chris breakpoint 1` or
`// mike breakpoint 2`. Lines that already have a breakpoint are skipped. You don't need
a debug session: CDT sends the breakpoints to GDB when the session starts.

It's the Eclipse port of the VS Code extension in `../test-vscode-extension`, and it needs
Eclipse with CDT (for example the "Eclipse IDE for C/C++ Developers" package).

## Choosing the name

- **On startup**, it scans every open project using that project's `name` setting
  (default `chris`).
- **From Quick Access** (Ctrl+3), **Comment Breakpoints: Set Breakpoints From Comments**
  asks for a name, pre-filled with the setting (from the active editor's project, if any),
  and scans every open project. A popup shows the result.
- **From a key binding that passes the name**, the prompt is skipped. Eclipse's Keys
  preference page can't pass arguments, so uncomment the `org.eclipse.ui.bindings` example
  in `plugin.xml`, set the key and name there, and rebuild. A binding made on the Keys page
  without a name works too; it just shows the prompt.

The name is matched literally (so `a.b` won't match `aXb`) and is case-sensitive.
Running it for one name doesn't remove breakpoints set for another.

## Settings

| Setting   | Default                                | Meaning                                  |
|-----------|----------------------------------------|------------------------------------------|
| `name`    | `chris`                                | Name in the marker comments              |
| `include` | `**/*.{c,h,cpp,hpp,cc,S,rs}`           | Files to scan (glob, relative to the project) |
| `exclude` | `**/{build,bsp,*_bsp,target,.git}/**`  | Files to skip (glob)                     |

- **For all projects:** Window > Preferences > Comment Breakpoints.
- **For one project:** right-click the project > Properties > Comment Breakpoints. That's
  stored in `<project>/.settings/local.commentbreakpoints.prefs` (the counterpart of
  `.vscode/settings.json`), which you can also write by hand:

  ```properties
  eclipse.preferences.version=1
  name=mike
  ```

## Output

The **Comment Breakpoints** console lists every breakpoint it added and a summary
(Console view > Open Console menu > Comment Breakpoints).

## Layout

```
META-INF/MANIFEST.MF     bundle metadata (id local.commentbreakpoints, version, dependencies)
plugin.xml               command, startup hook, preference and property pages
src/local/commentbreakpoints/
  Sync.java              the scan: marker regex, globs, adding the breakpoints
  SyncJob.java           runs a scan in the background
  Startup.java           scan on startup
  SyncHandler.java       the command
  Prefs.java             settings and their defaults
  PreferencePage.java    the preference / property page
  Log.java               the console
build.sh                 builds the jar without PDE
```

## Building the jar

```bash
./build.sh
```

This compiles against the plugins of your Eclipse install (`~/.p2/pool/plugins` and
`~/eclipse/*/eclipse/plugins` by default; pass another plugins folder as an argument) and
writes `local.commentbreakpoints_0.0.1.jar`. If you change the version, change
`Bundle-Version` in `META-INF/MANIFEST.MF`.

If your Eclipse has PDE, you can instead import this folder (File > Import > Existing
Projects into Workspace) and use File > Export > Deployable plug-ins and fragments.

## Installing and removing

Copy the jar into the install's `dropins` folder and restart Eclipse:

```bash
cp local.commentbreakpoints_0.0.1.jar ~/eclipse/cpp-latest-released/eclipse/dropins/
```

After a rebuild, replace the jar and restart with `-clean` once (`eclipse -clean`) so the
new code is loaded. To remove it, delete the jar from `dropins` and restart.

If no breakpoints show up after starting, check that Window > Preferences > General >
Startup and Shutdown has **Comment Breakpoints** ticked, and look at the console.
