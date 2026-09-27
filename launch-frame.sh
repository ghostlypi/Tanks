#!/bin/sh
# Launches Tanks natively on the Steam Frame (arm64 JDK, no FEX emulation).
#
# To use it from the Steam entry, set the game's launch options to:
#   /path/to/tanks/launch-frame.sh --steam %command%
# --steam discards Steam's original command (the x86 build), which is passed after it.
if [ "$1" = "--steam" ]; then
    set --
fi

cd "$(dirname "$0")"
JAVA="${JAVA:-$(ls -d "$HOME"/.local/jdk/jdk-17*/bin/java 2>/dev/null | head -n 1)}"
exec "${JAVA:-java}" -Dfile.encoding=UTF-8 -jar "$(ls build/libs/Tanks-*.jar | head -n 1)" "$@"
