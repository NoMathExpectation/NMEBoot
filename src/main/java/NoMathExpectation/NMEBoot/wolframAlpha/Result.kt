package NoMathExpectation.NMEBoot.wolframAlpha

import kotlinx.serialization.Serializable

@Serializable
internal data class Result(
    val result: String? = null,
    val conversationID: String,
    val host: String,
    val s: String? = null,
    val error: String? = null
)
