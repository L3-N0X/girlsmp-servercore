package de.lenox.servercore.commands

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

/**
 * Central place to register Brigadier commands.
 */
object Commands {
	fun init() {
		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			StatsCommand.register(dispatcher)
			ResourcePackCommand.register(dispatcher)
			VanishCommand.register(dispatcher)
			InvseeCommand.register(dispatcher)
		}
	}
}
