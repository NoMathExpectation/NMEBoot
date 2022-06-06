package NoMathExpectation.NMEBoot.RDLounge.rhythmCafe

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer

internal object MatchedTokensDeserializer: JsonTransformingSerializer<List<List<String>>>(ListSerializer(ListSerializer(String.serializer()))) {
    override fun transformDeserialize(element: JsonElement) = if (element is JsonArray) {
        if (element.isEmpty()) {
            JsonArray(listOf(JsonArray(listOf())))
        }
        if (element[0] is JsonArray) {
            element
        } else {
            JsonArray(listOf(element))
        }
    } else element
}