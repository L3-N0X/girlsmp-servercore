#!/usr/bin/env python3
"""
Cuts an image into tiles for a multi-glyph sprite (see Sprite.kt), e.g. the tab list banner:

    resourcepack/split_sprite.py banner.png banner 4

writes assets/girlsmp/textures/font/sprite/banner_0.png .. banner_3.png. The tile files (and the number of
tiles) have to match the bitmap providers in assets/girlsmp/font/sprite.json, so replacing the artwork with
an image of the same size needs no other change. Needs Pillow (pip install pillow).

Minecraft measures a bitmap glyph only up to its last non-transparent column. A tile whose right edge is fully
transparent would be drawn narrower and pull the next tiles to the left, so such tiles get one almost invisible
pixel (alpha 1) in their last column.
"""
import sys
from pathlib import Path

from PIL import Image

OUT_DIR = Path(__file__).resolve().parent / "assets/girlsmp/textures/font/sprite"


def main():
    if len(sys.argv) != 4:
        sys.exit("usage: split_sprite.py <image> <name> <tiles>")
    source, name, count = Path(sys.argv[1]), sys.argv[2], int(sys.argv[3])
    image = Image.open(source).convert("RGBA")
    if image.width % count:
        sys.exit(f"{source} is {image.width} px wide, which can't be split into {count} equal tiles")
    width = image.width // count

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for i in range(count):
        tile = image.crop((i * width, 0, (i + 1) * width, image.height))
        last = width - 1
        if i < count - 1 and all(tile.getpixel((last, y))[3] == 0 for y in range(tile.height)):
            r, g, b, _ = tile.getpixel((last, tile.height - 1))
            tile.putpixel((last, tile.height - 1), (r, g, b, 1))
            print(f"tile {i}: transparent right edge, added an alpha 1 pixel")
        out = OUT_DIR / f"{name}_{i}.png"
        tile.save(out, optimize=True)
        print(f"wrote {out.relative_to(Path.cwd()) if out.is_relative_to(Path.cwd()) else out} ({width}x{image.height})")


if __name__ == "__main__":
    main()
