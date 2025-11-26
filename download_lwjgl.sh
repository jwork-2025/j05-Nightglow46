#!/bin/bash
set -e

LWJGL_VERSION="3.3.6"
MAVEN_REPO="https://repo1.maven.org/maven2/org/lwjgl"
LIB_DIR="lib/lwjgl"

detect_platform() {
  case "$(uname -s)" in
    Darwin*) OS="macos";;
    Linux*)  OS="linux";;
    MINGW*|MSYS*|CYGWIN*|Windows*) OS="windows";;
    *) echo "Unsupported OS: $(uname -s)"; exit 1;;
  esac
  case "$(uname -m)" in
    arm64|aarch64) ARCH="arm64";;
    x86_64) ARCH="x86_64";;
    *) echo "Unsupported ARCH: $(uname -m)"; exit 1;;
  esac
}

fetch() {
  local url="$1" dest="$2"
  mkdir -p "$(dirname "$dest")"
  if command -v curl >/dev/null 2>&1; then
    curl -Ls -o "$dest" "$url"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$dest" "$url"
  else
    echo "Need curl or wget"; exit 1
  fi
}

download_module() {
  local module="$1" classifier="$2"
  local base="${MAVEN_REPO}/lwjgl${module}/${LWJGL_VERSION}/lwjgl${module}-${LWJGL_VERSION}"
  fetch "${base}.jar" "${LIB_DIR}/lwjgl${module}-${LWJGL_VERSION}.jar"
  fetch "${base}-natives-${classifier}.jar" "${LIB_DIR}/lwjgl${module}-${LWJGL_VERSION}-natives-${classifier}.jar"
}

extract_natives() {
  local classifier="$1"
  local natives_dir="${LIB_DIR}/natives/${classifier}"
  mkdir -p "$natives_dir"
  tmp="${LIB_DIR}/.tmp"
  mkdir -p "$tmp"
  for jar in "${LIB_DIR}"/*-natives-${classifier}.jar; do
    [ -f "$jar" ] || continue
    rm -rf "$tmp"/*
    if command -v unzip >/dev/null 2>&1; then
      unzip -q -o "$jar" -d "$tmp" || true
    elif command -v jar >/dev/null 2>&1; then
      (cd "$tmp" && jar xf "$jar") || true
    fi
    if [ -d "$tmp/$OS/$ARCH/org/lwjgl" ]; then
      mkdir -p "$natives_dir/org"
      cp -r "$tmp/$OS/$ARCH/org/lwjgl" "$natives_dir/org/" 2>/dev/null || true
    elif [ -d "$tmp/org/lwjgl" ]; then
      mkdir -p "$natives_dir/org"
      cp -r "$tmp/org/lwjgl" "$natives_dir/org/" 2>/dev/null || true
    fi
  done
  rm -rf "$tmp"
}

main() {
  detect_platform
  classifier="${OS}-${ARCH}"
  mkdir -p "$LIB_DIR"
  for m in "" "-glfw" "-opengl"; do
    download_module "$m" "$classifier"
  done
  extract_natives "$classifier"
  echo "LWJGL ${LWJGL_VERSION} downloaded to ${LIB_DIR}"
}

main "$@"

