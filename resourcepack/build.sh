#!/usr/bin/env bash
#
# Builds the resource pack zip: build/resourcepack/girlsmp-resourcepack.zip
#
#   resourcepack/build.sh [version]     (version defaults to "dev")
#
# The zip is reproducible (sorted entries, fixed timestamps, no extra attributes), so the same
# sources always give the same SHA-1. The release workflow (.github/workflows/resourcepack.yml)
# uses this script too; run it locally to test the pack (drop the zip into .minecraft/resourcepacks).

set -euo pipefail
# Zip stores local time; UTC keeps the timestamps (and so the hash) the same on every machine.
export TZ=UTC

VERSION="${1:-dev}"
PACK_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$(dirname "$PACK_DIR")/build/resourcepack"
OUT_FILE="$OUT_DIR/girlsmp-resourcepack.zip"
STAGING="$OUT_DIR/staging"

command -v zip >/dev/null || { echo "zip not found" >&2; exit 1; }

# Fail early on broken JSON instead of a pack that silently doesn't load on the client.
if command -v python3 >/dev/null; then
	find "$PACK_DIR/assets" -name '*.json' -print0 | xargs -0 -n 50 python3 -c '
import json, sys
for f in sys.argv[1:]:
	try: json.load(open(f, encoding="utf-8"))
	except Exception as e: sys.exit(f"invalid JSON in {f}: {e}")
'
fi

rm -rf "$STAGING" "$OUT_FILE"
mkdir -p "$STAGING"
cp -r "$PACK_DIR/assets" "$STAGING/"
[ -f "$PACK_DIR/pack.png" ] && cp "$PACK_DIR/pack.png" "$STAGING/"
sed "s/\${version}/$VERSION/g" "$PACK_DIR/pack.mcmeta" > "$STAGING/pack.mcmeta"

cd "$STAGING"
find . -exec touch -h -d '1980-01-01 00:00:00 UTC' {} +
find . -type f | sed 's|^\./||' | LC_ALL=C sort | zip -X -D -q -9 "$OUT_FILE" -@
cd - >/dev/null
rm -rf "$STAGING"

echo "Built $OUT_FILE ($(du -h "$OUT_FILE" | cut -f1)), version $VERSION"
echo "SHA-1: $(sha1sum "$OUT_FILE" | cut -d' ' -f1)"
