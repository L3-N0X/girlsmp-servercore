package de.lenox.servercore.core.resources

import de.lenox.servercore.ServerCore
import de.lenox.servercore.core.ServerModule
import de.lenox.servercore.core.onServerThread
import de.lenox.servercore.core.storage.JsonFileStore
import de.lenox.servercore.core.storage.Storage
import de.lenox.servercore.core.utils.components.Cmp
import de.lenox.servercore.core.utils.components.Theme
import de.lenox.servercore.core.utils.components.toNative
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.config.ServerResourcePackConfigurationTask
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Optional
import java.util.UUID

/**
 * Sends the server resource pack (built from `resourcepack/` by the release workflow) to every player.
 *
 * On startup (and on `/resourcepack reload`) the newest `pack-v*` release is looked up through the GitHub
 * API; its notes contain the SHA-1. Players get the pack in the configuration phase, the same way vanilla
 * sends the `resource-pack` from server.properties: it's downloaded and applied in the loading screen,
 * before they enter the world. If server.properties already sets a pack, this module stays out of the way.
 */
object ResourcePackModule : ServerModule {
	override val id = "resourcepack"

	private val configStore = JsonFileStore("config", ResourcePackConfig.serializer())
	private const val CONFIG_KEY = "resourcepack"

	/** Same id for every version, so a new version replaces the old one on the client. */
	private val PACK_ID: UUID = UUID.nameUUIDFromBytes("girlsmp:resourcepack".toByteArray())

	private val http = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build()
	private val json = Json { ignoreUnknownKeys = true }

	data class Pack(val version: String, val url: String, val sha1: String)

	/** The pack that joining players get, or null if there is none (disabled, not found, lookup failed). */
	@Volatile
	var current: Pack? = null
		private set

	@Volatile
	var config = ResourcePackConfig()
		private set

	// Runs once, when ServerCore registers this module.
	init {
		ServerConfigurationConnectionEvents.CONFIGURE.register { handler, server ->
			val pack = current ?: return@register
			if (server.serverResourcePack.isPresent) return@register
			handler.addTask(ServerResourcePackConfigurationTask(info(server, pack)))
		}
	}

	override fun onServerStarted(server: MinecraftServer) {
		if (server.serverResourcePack.isPresent) {
			ServerCore.logger.info("server.properties sets a resource pack, not sending the GitHub resource pack")
			return
		}
		Storage.scope.launch { reload(server) }
	}

	/**
	 * Re-reads the config and looks up the newest release. Returns the pack, or null if there is none.
	 * If [pushToOnline] is set and the pack changed, online players get the new version right away.
	 */
	suspend fun reload(server: MinecraftServer, pushToOnline: Boolean = false): Pack? {
		// Saving writes new options with their defaults into the file, so it always shows everything.
		val config = (configStore.load(CONFIG_KEY) ?: ResourcePackConfig()).also { configStore.save(CONFIG_KEY, it) }
		val pack = if (config.enabled) fetchLatest(config) else null
		server.onServerThread {
			val changed = pack != current
			this.config = config
			current = pack
			when {
				!config.enabled -> ServerCore.logger.info("Resource pack disabled in {}", configStore.directory)
				pack == null -> ServerCore.logger.warn("No resource pack release found in {}", config.repository)
				else -> ServerCore.logger.info("Resource pack {}: {}", pack.version, pack.url)
			}
			if (pushToOnline && changed && pack != null) {
				server.playerList.players.forEach { it.connection.send(pushPacket(server, pack)) }
			}
		}
		return pack
	}

	private fun info(server: MinecraftServer, pack: Pack) =
		MinecraftServer.ServerResourcePackInfo(PACK_ID, pack.url, pack.sha1, config.required, prompt(server))

	private fun pushPacket(server: MinecraftServer, pack: Pack) =
		ClientboundResourcePackPushPacket(PACK_ID, pack.url, pack.sha1, config.required, Optional.of(prompt(server)))

	private fun prompt(server: MinecraftServer) = Cmp(
		Cmp("GirlSMP", Theme.LIGHT_PINK, Theme.PINK),
		Cmp(" uses its own resource pack for fonts and icons.", Theme.SUBTEXT_1),
	).toNative(server)

	@Serializable
	private data class GitHubRelease(
		@SerialName("tag_name") val tagName: String,
		val body: String? = null,
		val draft: Boolean = false,
		val prerelease: Boolean = false,
	)

	/** Newest published release whose tag starts with [ResourcePackConfig.tagPrefix]. */
	private suspend fun fetchLatest(config: ResourcePackConfig): Pack? {
		val request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/${config.repository}/releases?per_page=30"))
			.header("Accept", "application/vnd.github+json")
			.header("User-Agent", ServerCore.MOD_ID)
			.timeout(Duration.ofSeconds(15))
			.build()
		val response = try {
			http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
		} catch (e: Exception) {
			ServerCore.logger.error("Could not reach the GitHub API for the resource pack: {}", e.toString())
			return null
		}
		if (response.statusCode() != 200) {
			ServerCore.logger.error("GitHub API returned {} for {}: {}", response.statusCode(), config.repository, response.body().take(200))
			return null
		}
		// The API lists releases newest first.
		val release = json.decodeFromString<List<GitHubRelease>>(response.body())
			.firstOrNull { !it.draft && !it.prerelease && it.tagName.startsWith(config.tagPrefix) }
			?: return null
		val sha1 = release.body?.let { SHA1_PATTERN.find(it)?.groupValues?.get(1)?.lowercase() }
		if (sha1 == null) {
			ServerCore.logger.error("Release {} has no 'SHA-1: <hash>' line in its notes", release.tagName)
			return null
		}
		val url = when (config.source) {
			DownloadSource.JSDELIVR -> "https://cdn.jsdelivr.net/gh/${config.repository}@${release.tagName}/${config.fileName}"
			DownloadSource.GITHUB -> "https://github.com/${config.repository}/releases/download/${release.tagName}/${config.fileName}"
		}
		return Pack(release.tagName.removePrefix(config.tagPrefix), url, sha1)
	}

	private val SHA1_PATTERN = Regex("SHA-1:\\s*([a-fA-F0-9]{40})")
}
