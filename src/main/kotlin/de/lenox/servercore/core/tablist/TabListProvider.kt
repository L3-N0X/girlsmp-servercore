package de.lenox.servercore.core.tablist

import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer

/** Content of the tab list, rendered every tick by [TabListModule]. */
interface TabListProvider {
	/** Text above the player list, per viewer; may span several lines (`\n`). */
	fun getHeader(player: ServerPlayer): Component = Component.empty()

	/** Text below the player list, per viewer. */
	fun getFooter(player: ServerPlayer): Component = Component.empty()

	/**
	 * Text right of [player]'s name, the same for every viewer (rendered once per player, not per viewer).
	 * Null shows nothing; while every player's score is null the vanilla list objective (if any) is shown instead.
	 */
	fun getScore(player: ServerPlayer): Component? = null
}
