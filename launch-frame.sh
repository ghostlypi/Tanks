#!/bin/sh
# Launches Tanks on the Steam Frame. Add this script to Steam as a non-Steam game
# so Steam Input hands the controllers to the game as a gamepad.
cd "$(dirname "$0")"
JAVA="${JAVA:-$(ls -d "$HOME"/.local/jdk/jdk-17*/bin/java 2>/dev/null | head -n 1)}"
exec "${JAVA:-java}" -Dfile.encoding=UTF-8 -jar "$(ls build/libs/Tanks-*.jar | head -n 1)" "$@"
