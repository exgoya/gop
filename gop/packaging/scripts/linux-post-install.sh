#!/bin/sh
set -e

BIN="/opt/gop/bin/gop"
LINK="/usr/local/bin/gop"

if [ -x "$BIN" ]; then
  mkdir -p "$(dirname "$LINK")"
  ln -sf "$BIN" "$LINK"
fi

exit 0
