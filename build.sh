#!/usr/bin/env bash
# Полная сборка: классы, проверки, jar. Артефакт: build/libs/zmeyka-<версия>.jar
set -euo pipefail
cd "$(dirname "$0")"

VERSION="$(sed -nE 's/.*NUMBER = "([^"]+)".*/\1/p' src/main/java/com/dzolotarev/zmeyka/Version.java)"
MAIN_CLASS="com.dzolotarev.zmeyka.Main"

rm -rf build/classes build/classes-test build/libs
mkdir -p build/classes build/classes-test build/libs

javac -encoding UTF-8 -d build/classes $(find src/main -name '*.java')
javac -encoding UTF-8 -cp build/classes -d build/classes-test $(find src/test -name '*.java')

java -cp build/classes:build/classes-test com.dzolotarev.zmeyka.GameLogicTest

jar --create --file "build/libs/zmeyka-$VERSION.jar" --main-class "$MAIN_CLASS" -C build/classes .
echo "jar: build/libs/zmeyka-$VERSION.jar"
