#!/usr/bin/env bash

set -euo pipefail

projdir=$(dirname "$0")

echo "  [compiling with mvn compile...]" >&2
mvn -f "$projdir" compile -q
cp=$(mvn exec:exec -Dexec.executable="echo" -Dexec.args="%classpath" -q)
echo "  [compilation complete; running main...]" >&2
java -cp "$cp" si413.Main "$@"
