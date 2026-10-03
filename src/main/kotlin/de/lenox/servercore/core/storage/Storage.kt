package de.lenox.servercore.core.storage

import de.lenox.servercore.ServerCore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Shared infrastructure for all file based persistence.
 *
 * Everything lives in `<server dir>/servercore/` as pretty printed JSON, so files can be edited by hand.
 * Disk access never happens on the server thread: [JsonFileStore] runs it on [scope].
 */
object Storage {
	/** `<server dir>/servercore`. */
	val root: Path = FabricLoader.getInstance().gameDir.resolve(ServerCore.MOD_ID)

	val json = Json {
		prettyPrint = true
		prettyPrintIndent = "  "
		// Always write every field, so hand-edited files show all available options.
		encodeDefaults = true
		// Tolerate hand edits: unknown fields are ignored, missing fields fall back to their defaults.
		ignoreUnknownKeys = true
		coerceInputValues = true
	}

	/** Background scope for disk I/O. A failing job is logged and doesn't cancel the others. */
	val scope = CoroutineScope(
		SupervisorJob() + Dispatchers.IO + CoroutineName("servercore-storage") +
			CoroutineExceptionHandler { _, e -> ServerCore.logger.error("Storage task failed", e) },
	)

	private val stores = CopyOnWriteArrayList<JsonFileStore<*>>()

	internal fun register(store: JsonFileStore<*>) {
		stores += store
	}

	/**
	 * Blocks until all queued reads/writes of every store are done. Only meant for server shutdown,
	 * so data isn't lost when the JVM exits.
	 */
	fun awaitPendingWrites(timeout: Duration = 10.seconds) {
		val finished = runBlocking {
			withTimeoutOrNull(timeout) { stores.forEach { it.flush() } }
		}
		if (finished == null) ServerCore.logger.warn("Timed out after {} waiting for pending storage writes", timeout)
	}
}
