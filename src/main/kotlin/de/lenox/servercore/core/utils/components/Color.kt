package de.lenox.servercore.core.utils.components

import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.util.HSVLike
import kotlin.math.roundToInt

/**
 * Creates a lighter version of the color.
 *
 * @param amount The percentage to lighten the color by (0-100). Defaults to 10.
 */
fun TextColor.lighter(amount: Int = 10): TextColor {
	val hsv = asHSV()
	return TextColor.color(HSVLike.hsvLike(hsv.h(), hsv.s(), (hsv.v() + amount / 100f).coerceAtMost(1f)))
}

/**
 * Creates a darker version of the color.
 *
 * @param amount The percentage to darken the color by (0-100). Defaults to 10.
 */
fun TextColor.darker(amount: Int = 10): TextColor {
	val hsv = asHSV()
	return TextColor.color(HSVLike.hsvLike(hsv.h(), hsv.s(), (hsv.v() - amount / 100f).coerceAtLeast(0f)))
}

/**
 * Generates a shade of the color, similar to Tailwind CSS shades.
 * - A value of 50 returns the original color.
 * - Values below 50 are tints (mixed with white). `shade(0)` is pure white.
 * - Values above 50 are shades (mixed with black). `shade(100)` is pure black.
 */
fun TextColor.shade(value: Int): TextColor {
	val clamped = value.coerceIn(0, 100)
	fun lerp(start: Int, end: Int, amount: Float) = (start + amount * (end - start)).roundToInt()
	return when {
		clamped < 50 -> {
			val amount = (50 - clamped) / 50f
			TextColor.color(lerp(red(), 255, amount), lerp(green(), 255, amount), lerp(blue(), 255, amount))
		}
		clamped > 50 -> {
			val amount = (clamped - 50) / 50f
			TextColor.color(lerp(red(), 0, amount), lerp(green(), 0, amount), lerp(blue(), 0, amount))
		}
		else -> this
	}
}

/**
 * Generates an analogous color palette (colors next to each other on the color wheel).
 *
 * @param amount Separation from -100 to 100 (mapped to -30°..30°). Around 30 is a good default.
 * @return `[shifted left, original, shifted right]`
 */
fun TextColor.analogous(amount: Int = 30): List<TextColor> {
	val hueShift = (amount.coerceIn(-100, 100) / 100f) * (30f / 360f)
	val hsv = asHSV()
	val left = HSVLike.hsvLike((hsv.h() - hueShift + 1f) % 1f, hsv.s(), hsv.v())
	val right = HSVLike.hsvLike((hsv.h() + hueShift) % 1f, hsv.s(), hsv.v())
	return listOf(TextColor.color(left), this, TextColor.color(right))
}

/** Shifts the color towards blue (more blue, less red). */
fun TextColor.cooler(amount: Int = 15): TextColor =
	TextColor.color((red() - amount).coerceAtLeast(0), green(), (blue() + amount).coerceAtMost(255))

/** Shifts the color towards yellow/red (more red, less blue). */
fun TextColor.warmer(amount: Int = 15): TextColor =
	TextColor.color((red() + amount).coerceAtMost(255), green(), (blue() - amount).coerceAtLeast(0))
