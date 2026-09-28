#!/bin/sh
# Launches Tanks natively on the Steam Frame (arm64 JDK, no FEX emulation).
#
# To use it from the Steam entry, set the game's launch options to:
#   /path/to/tanks/launch-frame.sh --steam %command%
# --steam discards Steam's original command (the x86 build), which is passed after it.
# With --steam, output goes to $TANKS_LOG (default /tmp/tanks-frame.log), since Steam does not show it anywhere.
if [ "$1" = "--steam" ]; then
    set --
    exec >"${TANKS_LOG:-/tmp/tanks-frame.log}" 2>&1
fi

cd "$(dirname "$0")"
JAVA="${JAVA:-$(ls -d "$HOME"/.local/jdk/jdk-17*/bin/java 2>/dev/null | head -n 1)}"
exec "${JAVA:-java}" -Dfile.encoding=UTF-8 -jar "$(ls -t build/libs/Tanks-*.jar | head -n 1)" "$@"
