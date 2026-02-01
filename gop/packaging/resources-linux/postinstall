#!/bin/sh
set -e

BIN="/opt/gop/bin/gop"
LINK="/usr/local/bin/gop"
CONFIG_SRC="/opt/gop/config"
CONFIG_DST="/etc/gop"
DATA_DIR="/var/lib/gop"
LOG_DIR="/var/log/gop"

if [ -x "$BIN" ]; then
  mkdir -p "$(dirname "$LINK")"
  ln -sf "$BIN" "$LINK"
fi

mkdir -p "$CONFIG_DST" "$DATA_DIR" "$LOG_DIR"

if [ -d "$CONFIG_SRC" ]; then
  for f in "$CONFIG_SRC"/*.json; do
    [ -f "$f" ] || continue
    base="$(basename "$f")"
    if [ ! -f "$CONFIG_DST/$base" ]; then
      cp "$f" "$CONFIG_DST/$base"
      chmod 0644 "$CONFIG_DST/$base" || true
    fi
  done
fi

exit 0
