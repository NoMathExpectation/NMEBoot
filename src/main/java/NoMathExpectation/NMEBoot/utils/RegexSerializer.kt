package NoMathExpectation.NMEBoot.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object RegexSerializer : KSerializer<Regex> {
    override val descriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder) = Regex(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Regex) = encoder.encodeString(value.pattern)
}