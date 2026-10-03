package de.lenox.servercore.core.bossbar

import de.lenox.servercore.core.ServerModule

/**
 * Custom per-player bossbars.
 *
 * TODO: manage a ServerBossEvent per player and add/remove players on join/leave.
 */
object BossBarModule : ServerModule {
	override val id = "bossbar"
}
