package NoMathExpectation.NMEBoot.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration

@Deprecated("There is already a official serializer for Duration.")
object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("duration", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Duration {
        return Duration.parse(decoder.decodeString())
    }
}

@Deprecated("There is already a official serializer for Duration.")
@Suppress("DEPRECATION")
typealias SerializableDuration = @Serializable(with = DurationSerializer::class) Duration