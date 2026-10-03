package de.lenox.servercore.core.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

/** Stores an [Instant] as a readable ISO-8601 string, e.g. `"2026-10-02T18:30:00Z"`. */
object InstantSerializer : KSerializer<Instant> {
	override val descriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())

	override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
