#!/bin/sh
set -e

BIN="/opt/gop/bin/gop"
LINK="/usr/local/bin/gop"

if [ -L "$LINK" ]; then
  TARGET="$(readlink "$LINK")"
  if [ "$TARGET" = "$BIN" ]; then
    rm -f "$LINK"
  fi
fi

exit 0
