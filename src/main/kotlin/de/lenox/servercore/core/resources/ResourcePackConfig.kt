package de.lenox.servercore.core.resources

import kotlinx.serialization.Serializable

/** `servercore/config/resourcepack.json`. Apply changes with `/resourcepack reload`. */
@Serializable
data class ResourcePackConfig(
	val enabled: Boolean = true,
	/** GitHub repository (`owner/name`) whose `pack-v*` releases contain the pack. Must be public. */
	val repository: String = "L3-N0X/girlsmp-servercore",
	val tagPrefix: String = "pack-v",
	val fileName: String = "girlsmp-resourcepack.zip",
	/** Where clients download the pack from. jsDelivr is a worldwide CDN but only serves files up to 20 MB. */
	val source: DownloadSource = DownloadSource.JSDELIVR,
	/** Players who decline a required pack can't join. */
	val required: Boolean = true,
)

enum class DownloadSource {
	/** `https://cdn.jsdelivr.net/gh/<repository>@<tag>/<fileName>` */
	JSDELIVR,

	/** `https://github.com/<repository>/releases/download/<tag>/<fileName>` */
	GITHUB,
}
