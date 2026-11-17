#!/usr/bin/env bash
# Compiles the plugin against an installed Eclipse CDT and packages it as a bundle jar.
# Usage: ./build.sh [plugins-dir ...]   (default: ~/.p2/pool/plugins and ~/eclipse/*/eclipse/plugins)
set -euo pipefail
cd "$(dirname "$0")"

dirs=("$@")
if [ ${#dirs[@]} -eq 0 ]; then
  dirs=("$HOME/.p2/pool/plugins")
  for d in "$HOME"/eclipse/*/eclipse/plugins; do [ -d "$d" ] && dirs+=("$d"); done
fi
cp=$(find "${dirs[@]}" -maxdepth 1 -name '*.jar' 2>/dev/null | tr '\n' ':')
if ! grep -q 'org.eclipse.cdt.debug.core_' <<<"$cp"; then
  echo "No CDT found in: ${dirs[*]} (pass the Eclipse plugins folder as an argument)" >&2
  exit 1
fi

version=$(sed -n 's/^Bundle-Version: *//p' META-INF/MANIFEST.MF | tr -d '\r')
out="local.commentbreakpoints_${version}.jar"
rm -rf bin "$out"
javac --release 21 -nowarn -d bin -cp "$cp" $(find src -name '*.java')
cp plugin.xml bin/
jar --create --file "$out" --manifest META-INF/MANIFEST.MF -C bin .
echo "built $out"
