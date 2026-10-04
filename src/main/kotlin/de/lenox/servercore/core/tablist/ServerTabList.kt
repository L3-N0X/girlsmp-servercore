package de.lenox.servercore.core.tablist

import de.lenox.servercore.core.resources.Font
import de.lenox.servercore.core.resources.Sprite
import de.lenox.servercore.core.stats.PlayerStatsModule
import de.lenox.servercore.core.utils.components.Cmp
import de.lenox.servercore.core.utils.components.Theme
import net.kyori.adventure.text.Component
import net.minecraft.server.level.ServerPlayer

/** Default tab list: the server banner above the player list and everyone's death count next to their name. */
object ServerTabList : TabListProvider {
	// Built once instead of every tick.
	private val header = Sprite.BANNER.toBlock(Sprite.Size.BASE)
	private val skull = Cmp(Sprite.SKULL.toComponent(Sprite.Size.TEXT), Cmp.space(Cmp.SpaceSize.ONE))

	override fun getHeader(player: ServerPlayer): Component = header

	/** `☠ 3`; nothing while the player's stats are still loading. */
	override fun getScore(player: ServerPlayer): Component? {
		val deaths = PlayerStatsModule.online(player.uuid)?.deaths ?: return null
		return Cmp(skull, Cmp(deaths.toString(), Theme.LIGHT_RED, Font.MONO))
	}
}
