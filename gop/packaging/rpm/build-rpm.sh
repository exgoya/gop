#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-0.0.0}"
APP_IMAGE_DIR="${2:-}"
OUT_DIR="${3:-}"

if [[ -z "$APP_IMAGE_DIR" || -z "$OUT_DIR" ]]; then
  echo "usage: $0 <version> <app-image-dir> <out-dir>"
  exit 1
fi

mkdir -p "$OUT_DIR"
OUT_DIR="$(cd "$OUT_DIR" && pwd)"
APP_IMAGE_DIR="$(cd "$APP_IMAGE_DIR" && pwd)"

TOPDIR="$OUT_DIR"
SPECS_DIR="$TOPDIR/SPECS"
BUILD_DIR="$TOPDIR/BUILD"
RPMS_DIR="$TOPDIR/RPMS"
SRPMS_DIR="$TOPDIR/SRPMS"

mkdir -p "$SPECS_DIR" "$BUILD_DIR" "$RPMS_DIR" "$SRPMS_DIR"

SPEC_SRC="$(dirname "$0")/gop.spec.in"
SPEC_DST="$SPECS_DIR/gop.spec"

sed "s/@VERSION@/$VERSION/g" "$SPEC_SRC" > "$SPEC_DST"

APP_IMAGE_TARGET="$BUILD_DIR/app-image"
rm -rf "$APP_IMAGE_TARGET"
mkdir -p "$APP_IMAGE_TARGET"
cp -a "$APP_IMAGE_DIR"/. "$APP_IMAGE_TARGET"/

rpmbuild -bb "$SPEC_DST" \
  --define "_topdir $TOPDIR" \
  --define "_builddir $BUILD_DIR" \
  --define "_rpmdir $RPMS_DIR" \
  --define "_srcrpmdir $SRPMS_DIR"
