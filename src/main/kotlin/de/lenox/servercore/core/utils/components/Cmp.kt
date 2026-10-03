package de.lenox.servercore.core.utils.components

import de.lenox.servercore.core.resources.Font
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * Builds Adventure components, same API as Eventrox's `Cmp`.
 *
 * ```
 * player.sendMessage(Cmp(Cmp("» ", Theme.SUBTEXT_3), Cmp("Hello", Theme.LIGHT_PURPLE, Theme.LIGHT_BLUE)))
 * ```
 *
 * Gradients are built directly (one colour per character) instead of going through MiniMessage, so the
 * text is never parsed: player names or other input can't inject tags.
 */
object Cmp {
	private val miniMessage = MiniMessage.miniMessage()

	/** Creates a colored text component, optionally without the text shadow. */
	operator fun invoke(text: String, color: TextColor, noShadow: Boolean = false): Component =
		Component.text(text, color).noShadow(noShadow)

	/** Creates a colored text component with a custom font (e.g. from a resource pack). */
	operator fun invoke(text: String, color: TextColor, font: Key, noShadow: Boolean = false): Component =
		Component.text(text, color).font(font).noShadow(noShadow)

	/** Creates a colored text component with a custom font and shadow color. */
	operator fun invoke(text: String, color: TextColor, font: Key, shadowColor: ShadowColor): Component =
		Component.text(text, color).font(font).shadowColor(shadowColor)

	/** Creates an uncolored text component with a custom font. */
	operator fun invoke(text: String, font: Key): Component = Component.text(text).font(font)

	/** Creates an uncolored text component, optionally without the text shadow. */
	operator fun invoke(text: String, noShadow: Boolean = false): Component = Component.text(text).noShadow(noShadow)

	/** Creates a gradient from two hex colors, e.g. `Cmp("Hello", 0xB6ABFB, 0x7496E3)`. */
	operator fun invoke(text: String, startColor: Int, endColor: Int): Component =
		gradient(text, TextColor.color(startColor), TextColor.color(endColor))

	/** Creates a gradient from [startColor] to [endColor]. */
	operator fun invoke(text: String, startColor: TextColor, endColor: TextColor): Component =
		gradient(text, startColor, endColor)

	/** Creates a gradient with a custom font. */
	operator fun invoke(text: String, startColor: TextColor, endColor: TextColor, font: Key): Component =
		gradient(text, startColor, endColor).font(font)

	/** Combines multiple components into one, in order. */
	operator fun invoke(vararg c: Component): Component = invoke(c.asList())

	/** Combines a list of components into one, in order. */
	operator fun invoke(components: List<Component>): Component =
		Component.text().append(components).build()

	/**
	 * Colors [text] with a gradient through all [colors] (at least two), spread evenly over the characters.
	 * Neighbouring characters that end up with the same color share one component.
	 */
	fun gradient(text: String, vararg colors: TextColor): Component {
		require(colors.size >= 2) { "a gradient needs at least two colors" }
		val chars = text.codePoints().toArray()
		if (chars.isEmpty()) return Component.empty()

		val builder = Component.text()
		val run = StringBuilder()
		var runColor: TextColor? = null
		chars.forEachIndexed { index, char ->
			val progress = if (chars.size == 1) 0f else index.toFloat() / (chars.size - 1)
			val color = colorAt(progress, colors)
			if (color != runColor && run.isNotEmpty()) {
				builder.append(Component.text(run.toString(), runColor))
				run.clear()
			}
			runColor = color
			run.appendCodePoint(char)
		}
		builder.append(Component.text(run.toString(), runColor))
		return builder.build()
	}

	/** Converts [text] to small caps and applies a gradient. */
	fun smallCaps(text: String, startColor: TextColor, endColor: TextColor): Component =
		gradient(stringToSmallCaps(text), startColor, endColor)

	/** Converts [text] to small caps with a single color. */
	fun smallCaps(text: String, color: TextColor): Component = invoke(stringToSmallCaps(text), color)

	/** Creates a component with [lines] newlines. */
	fun newline(lines: Int = 1): Component {
		require(lines >= 1) { "lines must be at least 1" }
		return Component.text("\n".repeat(lines))
	}

	/**
	 * Creates a space of [size] pixels (-8192..8192, negative moves left), e.g. to align or overlap text.
	 * Needs the resource pack; it's a translation key, so without the pack the key text would show.
	 */
	fun space(size: Long): Component {
		require(size in -8192..8192) { "size must be between -8192 and 8192" }
		return Component.translatable("space.$size").font(Font.DEFAULT)
	}

	/** Creates a predefined space, e.g. `"-infinity"`, `"-max"`, `"1/2"`, `"-1/3"` (see `assets/space/lang`). */
	fun space(size: String): Component = Component.translatable("space.$size").font(Font.DEFAULT)

	/** Predefined spaces as characters of the space font, no translation needed. */
	enum class SpaceSize(val unicode: String) {
		INFINITY("\uDB3F\uDFFF"),
		NEGATIVE_INFINITY("\uDAC0\uDC01"),
		NEWLAYER("\uDAC0\uDC00"),
		MAX("\uDB08\uDC00"),
		HALF("\uD902\uDD60"),
		ONE("\uDB00\uDC01"),
		TWO("\uDB00\uDC02"),
		THREE("\uDB00\uDC03"),
		FOUR("\uDB00\uDC04"),
		FIVE("\uDB00\uDC05"),
		SIX("\uDB00\uDC06"),
	}

	/** Creates a predefined space that also renders before the translations of the pack are loaded. */
	fun space(size: SpaceSize): Component = Component.text(size.unicode).font(Font.SPACE)

	/**
	 * Parses a MiniMessage string, e.g. `<gradient:#B6ABFB:#7496E3>Hello</gradient>`.
	 * Meant for trusted strings like config values. Pass player input as a placeholder
	 * ([net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed]), never by concatenating it in.
	 */
	fun mini(text: String, vararg resolvers: TagResolver): Component = miniMessage.deserialize(text, *resolvers)

	private fun colorAt(progress: Float, colors: Array<out TextColor>): TextColor {
		val position = progress * (colors.size - 1)
		val index = position.toInt().coerceAtMost(colors.size - 2)
		return TextColor.lerp(position - index, colors[index], colors[index + 1])
	}

	private fun Component.noShadow(noShadow: Boolean): Component = if (noShadow) shadowColor(ShadowColor.none()) else this

	private val smallCapsMap = mapOf(
		'a' to 'ᴀ', 'b' to 'ʙ', 'c' to 'ᴄ', 'd' to 'ᴅ', 'e' to 'ᴇ', 'f' to 'ꜰ', 'g' to 'ɢ', 'h' to 'ʜ', 'i' to 'ɪ',
		'j' to 'ᴊ', 'k' to 'ᴋ', 'l' to 'ʟ', 'm' to 'ᴍ', 'n' to 'ɴ', 'o' to 'ᴏ', 'p' to 'ᴘ', 'q' to 'ǫ', 'r' to 'ʀ',
		's' to 's', 't' to 'ᴛ', 'u' to 'ᴜ', 'v' to 'ᴠ', 'w' to 'ᴡ', 'x' to 'x', 'y' to 'ʏ', 'z' to 'ᴢ',
	)

	private fun stringToSmallCaps(text: String): String = text.map { smallCapsMap[it.lowercaseChar()] ?: it }.joinToString("")
}

/** Converts a String to a Component. */
fun String.toComponent(): Component = Component.text(this)

/** Converts a String to a colored Component. */
fun String.toColoredCmp(color: TextColor, noShadow: Boolean = false): Component = Cmp(this, color, noShadow)

/** Converts a String to a gradient Component. */
fun String.toColoredCmp(startColor: TextColor, endColor: TextColor): Component = Cmp(this, startColor, endColor)

/** Converts a String to a gradient Component through all [colors]. */
fun String.gradient(vararg colors: TextColor): Component = Cmp.gradient(this, *colors)

/** Repeats this component [times] times (at least 1). */
fun Component.repeat(times: Int): Component {
	require(times >= 1) { "times must be at least 1" }
	return Cmp(List(times) { this })
}
