#!/usr/bin/env bash
# Только проверки логики (без GUI и без сборки jar).
set -euo pipefail
cd "$(dirname "$0")"

rm -rf build/classes build/classes-test
mkdir -p build/classes build/classes-test
javac -encoding UTF-8 -d build/classes $(find src/main -name '*.java')
javac -encoding UTF-8 -cp build/classes -d build/classes-test $(find src/test -name '*.java')
java -cp build/classes:build/classes-test com.dzolotarev.zmeyka.GameLogicTest
