package de.lenox.servercore.core.resources

import net.kyori.adventure.key.Key

/**
 * Fonts of the server resource pack (`resourcepack/assets/<namespace>/font/`), same as Eventrox's `Font`
 * but in the `girlsmp` namespace. Everything except [DEFAULT] needs the pack, which [ResourcePackModule]
 * sends to every player.
 */
object Font {
	val DEFAULT = Key.key("minecraft:default")

	/** Monospaced default font (6 px per character): numbers and columns line up. */
	val MONO = Key.key("girlsmp:mono")

	/** Monospaced, slightly bigger and lower; for titles. */
	val BIG_MONO = Key.key("girlsmp:big_mono")
	val BIG = Key.key("girlsmp:big")

	/** Pixel caps: lowercase letters become small caps, uppercase stay full caps. */
	val CAPS = Key.key("girlsmp:caps")

	/** [CAPS], vertically centred (e.g. inside button backgrounds). */
	val CAPS_CENTER = Key.key("girlsmp:caps_center")

	/** The vanilla font, drawn bigger. */
	val DEFAULT_XL = Key.key("girlsmp:default_xl")

	/** Small caps TTF, shifted up by 9 px (a second line above normal text). */
	val SHIFT_UP = Key.key("girlsmp:shift_up")

	/** Negative/positive spaces, see `Cmp.space`. */
	val SPACE = Key.key("space:default")

	/** Pixel fractions (`pixel.*` translation keys), for bars. */
	val PIXELIZED = Key.key("pixelized:pixelized")
}

/**
 * Lowercases the text and replaces digits with the full-height digits of [Font.CAPS] (``-``),
 * so numbers are as high as the caps letters around them.
 */
fun String.capsWithLargeDigits(): String =
	lowercase().map { if (it in '0'..'9') '' + it.digitToInt() else it }.joinToString("")
