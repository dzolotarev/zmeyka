#!/usr/bin/env bash
# Быстрый запуск из исходников. Аргументы проходят в игру:
# ./run.sh --wrap --cols=40 --rows=28 --cell=20 --speed=120
set -euo pipefail
cd "$(dirname "$0")"

mkdir -p build/classes
javac -encoding UTF-8 -d build/classes $(find src/main -name '*.java')
java -cp build/classes com.dzolotarev.zmeyka.Main "$@"
