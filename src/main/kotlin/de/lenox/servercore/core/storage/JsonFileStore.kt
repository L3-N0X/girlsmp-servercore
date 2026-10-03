package de.lenox.servercore.core.storage

import de.lenox.servercore.ServerCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * A folder of JSON files, one file per key: `<server dir>/servercore/<folder>/<key>.json`.
 *
 * - **Async:** every operation runs off the server thread. Suspend functions can be awaited from a
 *   coroutine; [saveAsync] is fire-and-forget for callers on the server thread.
 * - **Ordered:** all operations of one store run one after another in call order, so a [load] always
 *   sees the latest [save] and two saves of the same key can't overtake each other.
 * - **Crash safe:** files are written to a temp file first and then atomically moved into place,
 *   so a crash mid-write never leaves a half written file behind.
 * - **Hand-edit friendly:** a file that fails to parse is renamed to `<key>.json.broken-<timestamp>`
 *   (instead of being silently overwritten) and treated as missing.
 */
class JsonFileStore<T : Any>(
	folder: String,
	private val serializer: KSerializer<T>,
) {
	val directory: Path = Storage.root.resolve(folder)

	// limitedParallelism(1) executes tasks in submission order, which gives the ordering guarantee above.
	@OptIn(ExperimentalCoroutinesApi::class)
	private val dispatcher = Dispatchers.IO.limitedParallelism(1)

	init {
		Storage.register(this)
	}

	suspend fun load(key: String): T? = withContext(dispatcher) { read(key) }

	/** Loads every file in the folder. Broken files are skipped (see class docs). */
	suspend fun loadAll(): Map<String, T> = withContext(dispatcher) {
		if (!directory.exists()) return@withContext emptyMap()
		directory.listDirectoryEntries("*.$EXTENSION")
			.mapNotNull { file -> read(file.nameWithoutExtension)?.let { file.nameWithoutExtension to it } }
			.toMap()
	}

	suspend fun save(key: String, value: T) = withContext(dispatcher) { write(key, value) }

	/** Queues a save without waiting for it. Safe to call from the server thread. */
	fun saveAsync(key: String, value: T): Job = Storage.scope.launch(dispatcher) { write(key, value) }

	/**
	 * Atomic read-modify-write: no other operation of this store can run between reading the
	 * current value (null if there's no file yet) and writing the result of [transform].
	 */
	suspend fun update(key: String, transform: (T?) -> T): T = withContext(dispatcher) {
		transform(read(key)).also { write(key, it) }
	}

	suspend fun delete(key: String): Boolean = withContext(dispatcher) { fileOf(key).deleteIfExists() }

	/** Suspends until everything queued before this call has finished. */
	internal suspend fun flush() = withContext(dispatcher) {}

	private fun read(key: String): T? {
		val file = fileOf(key)
		if (!file.exists()) return null
		return try {
			Storage.json.decodeFromString(serializer, file.readText())
		} catch (e: IllegalArgumentException) { // includes SerializationException
			quarantine(file, e)
			null
		}
	}

	private fun write(key: String, value: T) {
		val file = fileOf(key)
		directory.createDirectories()
		val tmp = Files.createTempFile(directory, "$key.", ".tmp")
		try {
			tmp.writeText(Storage.json.encodeToString(serializer, value) + "\n")
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
			} catch (_: AtomicMoveNotSupportedException) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING)
			}
		} catch (e: IOException) {
			tmp.deleteIfExists()
			throw e
		}
	}

	private fun quarantine(file: Path, cause: Exception) {
		val backup = file.resolveSibling("${file.fileName}.broken-${System.currentTimeMillis()}")
		ServerCore.logger.error("Could not parse {}, moved it to {}: {}", file, backup.fileName, cause.message)
		try {
			file.moveTo(backup)
		} catch (e: IOException) {
			ServerCore.logger.error("Could not move broken file {}", file, e)
		}
	}

	private fun fileOf(key: String): Path {
		require(KEY_PATTERN.matches(key)) { "Invalid storage key '$key'" }
		return directory.resolve("$key.$EXTENSION")
	}

	private companion object {
		const val EXTENSION = "json"

		/** Keys become file names, so keep them to a safe character set (no path separators, no leading dot). */
		val KEY_PATTERN = Regex("[A-Za-z0-9_-][A-Za-z0-9_.-]*")
	}
}
