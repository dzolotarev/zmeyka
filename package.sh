#!/usr/bin/env bash
# Дистрибутив: ELF-лаунчер + урезанный JRE (jlink) в build/dist/zmeyka/ и tar.gz для релиза.
# Maven/Gradle не используются: javac + jar + jpackage из JDK.
set -euo pipefail
cd "$(dirname "$0")"

VERSION="$(sed -nE 's/.*NUMBER = "([^"]+)".*/\1/p' src/main/java/com/dzolotarev/zmeyka/Version.java)"
./build.sh

JAR="zmeyka-$VERSION.jar"

rm -rf build/dist
mkdir -p build/dist
jpackage \
  --type app-image \
  --name zmeyka \
  --app-version "$VERSION" \
  --vendor dzolotarev \
  --input build/libs \
  --main-jar "$JAR" \
  --main-class com.dzolotarev.zmeyka.Main \
  --add-modules java.base,java.desktop \
  --dest build/dist \
  --jlink-options "--strip-debug --no-header-files --no-man-pages --compress=2" \
  --java-options -Dsun.java2d.uiScale=1.0

tar -czf "build/dist/zmeyka-$VERSION-linux-x64.tar.gz" -C build/dist zmeyka

echo "elf:  $(file -b build/dist/zmeyka/bin/zmeyka)"
echo "версия из elf: $(build/dist/zmeyka/bin/zmeyka --version)"
echo "размер app-image: $(du -sh build/dist/zmeyka | cut -f1)"
echo "tar.gz: build/dist/zmeyka-$VERSION-linux-x64.tar.gz ($(du -h "build/dist/zmeyka-$VERSION-linux-x64.tar.gz" | cut -f1))"
