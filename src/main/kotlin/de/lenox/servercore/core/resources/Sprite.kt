package de.lenox.servercore.core.resources

import de.lenox.servercore.core.utils.components.Cmp
import de.lenox.servercore.core.utils.components.Theme
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

/**
 * Every image of the resource pack, same idea as Eventrox's `SpriteComponent`. A sprite is one or more glyphs
 * of [Font.SPRITE] (`resourcepack/assets/girlsmp/font/sprite.json`) per [Size]; the same PNG is usually
 * declared several times with a different `height`/`ascent` to get the sizes.
 *
 * Images too wide for one glyph (e.g. [BANNER]) are cut into tiles (`resourcepack/split_sprite.py`), one glyph
 * each, and drawn side by side with [tiles]. To add a sprite: add the PNG and a `bitmap` provider per size to
 * `sprite.json`, then an entry here with the same code points.
 *
 * Glyphs draw upwards from the baseline when `ascent` = `height`, so a sprite that is taller than one line
 * (9 px) needs empty lines above it, see [Size]. [Size.TEXT] glyphs sit on the baseline like letters instead.
 */
enum class Sprite(private val glyphs: Map<Size, String>) {
	/** Server banner on top of the tab list, `textures/font/sprite/banner_0..3.png` (4 tiles of 64x64). */
	BANNER(
		Size.SMALL to tiles('', 4),
		Size.BASE to tiles('', 4),
		Size.LARGE to tiles('', 4),
	),

	/** Death counter icon next to the names in the tab list, `textures/font/sprite/skull.png` (8x8). */
	SKULL(Size.TEXT to ""),
	;

	constructor(vararg glyphs: Pair<Size, String>) : this(glyphs.toMap())

	/** Heights used in `sprite.json`; [lines] is how many lines of text the sprite covers (9 px each). */
	enum class Size(val height: Int, val lines: Int) {
		/** As high as a letter (`ascent` 7 instead of 8, like the default font), for icons inside a line of text. */
		TEXT(8, 1),
		SMALL(16, 2),
		BASE(32, 4),
		LARGE(48, 6),
	}

	/** The glyphs of [size], or of [Size.BASE] / the first declared size if the sprite doesn't have it. */
	fun unicode(size: Size = Size.BASE): String =
		glyphs[size] ?: glyphs[Size.BASE] ?: glyphs.values.first()

	/** White and without shadow, so the image keeps its own colours; [textColor] tints it instead. */
	fun toComponent(size: Size = Size.BASE, textColor: TextColor? = null): Component =
		Cmp(unicode(size), textColor ?: Theme.WHITE, Font.SPRITE, noShadow = true)

	/**
	 * [toComponent] preceded by enough empty lines that the sprite doesn't overlap the text above it, for
	 * multi-line texts like the tab list header. The sprite itself stays on the last line.
	 */
	fun toBlock(size: Size = Size.BASE, textColor: TextColor? = null): Component =
		if (size.lines > 1) Cmp(Cmp.newline(size.lines - 1), toComponent(size, textColor)) else toComponent(size, textColor)
}

/**
 * Moves 1 px back (a `space` provider in `sprite.json`). Every bitmap glyph advances 1 px more than it is
 * wide, this closes the gap between tiles.
 */
private const val TILE_JOINER = "\uf801"

// Top level instead of in a companion: enum entries are created before the companion is initialized.
/** [count] consecutive code points starting at [first], joined so the tiles touch. */
private fun tiles(first: Char, count: Int): String =
	(0 until count).joinToString(TILE_JOINER) { (first + it).toString() }
