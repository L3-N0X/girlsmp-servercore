package de.lenox.servercore

import de.lenox.servercore.commands.Commands
import de.lenox.servercore.core.ModuleManager
import de.lenox.servercore.core.bossbar.BossBarModule
import de.lenox.servercore.core.resources.ResourcePackModule
import de.lenox.servercore.core.scoreboard.ScoreboardModule
import de.lenox.servercore.core.stats.PlayerStatsModule
import de.lenox.servercore.core.stats.StatsSidebar
import de.lenox.servercore.core.tablist.ServerTabList
import de.lenox.servercore.core.tablist.TabListModule
import de.lenox.servercore.core.vanish.VanishModule
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object ServerCore : ModInitializer {
	const val MOD_ID = "servercore"
	val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		ModuleManager.register(
			TabListModule,
			BossBarModule,
			PlayerStatsModule,
			ScoreboardModule,
			ResourcePackModule,
			VanishModule,
		)
		ScoreboardModule.provider = StatsSidebar
		TabListModule.provider = ServerTabList
		ModuleManager.init()
		Commands.init()

		logger.info("Server Core initialized")
	}
}
