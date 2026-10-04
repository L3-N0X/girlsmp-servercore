package de.lenox.servercore.core.stats

import de.lenox.servercore.core.storage.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Persistent stats of one player, stored in `servercore/playerstats/<uuid>.json`.
 * All fields have defaults, so hand-edited files may leave out anything they don't care about.
 */
@Serializable
data class PlayerStats(
	/** Last known name; only informational, the file name (UUID) is what identifies the player. */
	val name: String = "",
	val playtimeSeconds: Long = 0,
	val joins: Int = 0,
	val deaths: Int = 0,
	/** Totems of undying that saved the player from dying. */
	val totemPops: Int = 0,
	@Serializable(InstantSerializer::class)
	val firstJoin: Instant? = null,
	@Serializable(InstantSerializer::class)
	val lastSeen: Instant? = null,
)
