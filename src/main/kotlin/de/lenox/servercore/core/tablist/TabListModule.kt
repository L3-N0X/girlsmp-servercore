package de.lenox.servercore.core.tablist

import de.lenox.servercore.core.ServerModule

/**
 * Custom tab list (header, footer, player names).
 *
 * TODO: send ClientboundTabListPacket for header/footer and
 *  ClientboundPlayerInfoUpdatePacket for custom display names.
 */
object TabListModule : ServerModule {
	override val id = "tablist"
}
