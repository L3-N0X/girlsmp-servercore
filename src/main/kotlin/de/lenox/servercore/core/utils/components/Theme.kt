package de.lenox.servercore.core.utils.components

import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.util.HSVLike
import kotlin.random.Random

/**
 * The colour palette, same as Eventrox. Every hue comes in `LIGHT_`, normal and `DARK_` variants.
 * Use these instead of the vanilla [net.minecraft.ChatFormatting] colours.
 */
object Theme {
	val LIGHT_PINK = TextColor.color(0xF5ABE4)
	val PINK = TextColor.color(0xD677C3)
	val DARK_PINK = TextColor.color(0xA94497)

	val LIGHT_ROSE = TextColor.color(0xFCB6D6)
	val ROSE = TextColor.color(0xDD84AF)
	val DARK_ROSE = TextColor.color(0xAF5382)

	val LIGHT_RED = TextColor.color(0xF88A82)
	val RED = TextColor.color(0xDE4E4B)
	val DARK_RED = TextColor.color(0xAC0018)

	val LIGHT_ORANGE = TextColor.color(0xFF9E77)
	val ORANGE = TextColor.color(0xE96326)
	val DARK_ORANGE = TextColor.color(0x9E4700)

	val LIGHT_OCKER = TextColor.color(0xFFCC9C)
	val OCKER = TextColor.color(0xF29520)
	val DARK_OCKER = TextColor.color(0xAD7000)

	val LIGHT_BRONZE = TextColor.color(0xD9AF75)
	val BRONZE = TextColor.color(0xB9832C)
	val DARK_BRONZE = TextColor.color(0x835A00)

	val LIGHT_GOLD = TextColor.color(0xFFE6A6)
	val GOLD = TextColor.color(0xE1B942)
	val DARK_GOLD = TextColor.color(0xAE8A00)

	val LIGHT_YELLOW = TextColor.color(0xFEF795)
	val YELLOW = TextColor.color(0xEEE354)
	val DARK_YELLOW = TextColor.color(0xC0B200)

	val LIGHT_LIME = TextColor.color(0xD2FFB9)
	val LIME = TextColor.color(0x95DB6C)
	val DARK_LIME = TextColor.color(0x62AC2A)

	val LIGHT_BRIGHT_GREEN = TextColor.color(0x9CEEAD)
	val BRIGHT_GREEN = TextColor.color(0x55C975)
	val DARK_BRIGHT_GREEN = TextColor.color(0x009A46)

	val LIGHT_GREEN = TextColor.color(0x89CB8A)
	val GREEN = TextColor.color(0x4AA651)
	val DARK_GREEN = TextColor.color(0x00791C)

	val LIGHT_MINT = TextColor.color(0xAEFCD9)
	val MINT = TextColor.color(0x69D6AA)
	val DARK_MINT = TextColor.color(0x18A87B)

	val LIGHT_TEAL = TextColor.color(0x8FEBDF)
	val TEAL = TextColor.color(0x36C6B8)
	val DARK_TEAL = TextColor.color(0x009589)

	val LIGHT_AQUA = TextColor.color(0xC7F1FF)
	val AQUA = TextColor.color(0x25D2FC)
	val DARK_AQUA = TextColor.color(0x009FC2)

	val LIGHT_AZURE = TextColor.color(0x83BDF8)
	val AZURE = TextColor.color(0x4393E1)
	val DARK_AZURE = TextColor.color(0x0064B2)

	val LIGHT_BLUE = TextColor.color(0x7496E3)
	val BLUE = TextColor.color(0x426BCE)
	val DARK_BLUE = TextColor.color(0x173AA4)

	val LIGHT_PURPLE = TextColor.color(0xB6ABFB)
	val PURPLE = TextColor.color(0x8F7CE3)
	val DARK_PURPLE = TextColor.color(0x654BB9)

	val LIGHT_MAGENTA = TextColor.color(0xE5AFFD)
	val MAGENTA = TextColor.color(0xC37DE2)
	val DARK_MAGENTA = TextColor.color(0x9749B6)

	// --- UTILITY & SUBTEXT COLORS ---
	val TEXT = TextColor.color(0xffffff)
	val WHITE = TextColor.color(0xffffff)
	val SUBTEXT_1 = TextColor.color(0xcdd6f4)
	val SUBTEXT_2 = TextColor.color(0xa6adc8)
	val SUBTEXT_3 = TextColor.color(0x6c7086)
	val LIGHTER_GRAY = TextColor.color(0xD9D9D9)
	val SILVER = TextColor.color(0xBDC9CC)
	val GRAY = TextColor.color(0x9FA5A7)
	val DARK_GRAY = TextColor.color(0x1e1e2e)
	val BLACK = TextColor.color(0x11111b)
	val PURE_BLACK = TextColor.color(0x000000)

	val MC_BLACK = TextColor.color(0x000000)
	val MC_DARK_BLUE = TextColor.color(0x0000aa)
	val MC_DARK_GREEN = TextColor.color(0x00aa00)
	val MC_DARK_AQUA = TextColor.color(0x00aaaa)
	val MC_DARK_RED = TextColor.color(0xaa0000)
	val MC_DARK_PURPLE = TextColor.color(0xaa00aa)
	val MC_GOLD = TextColor.color(0xffaa00)
	val MC_GRAY = TextColor.color(0xaaaaaa)
	val MC_DARK_GRAY = TextColor.color(0x555555)
	val MC_BLUE = TextColor.color(0x5555ff)
	val MC_GREEN = TextColor.color(0x55ff55)
	val MC_AQUA = TextColor.color(0x55ffff)
	val MC_RED = TextColor.color(0xff5555)
	val MC_LIGHT_PURPLE = TextColor.color(0xff55ff)
	val MC_YELLOW = TextColor.color(0xffff55)
	val MC_WHITE = TextColor.color(0xffffff)

	/**
	 * A random pastel colour; the same [seed] always gives the same colour (0 = truly random).
	 * Gives the same colour as Eventrox's `Theme.random` for the same seed.
	 */
	fun random(seed: Int = 0): TextColor {
		val value = Random(if (seed == 0) Random.nextInt() else seed).nextFloat()
		return TextColor.color(HSVLike.hsvLike(value, value * 0.45f + 0.2f, value * 0.1f + 0.8f))
	}
}
