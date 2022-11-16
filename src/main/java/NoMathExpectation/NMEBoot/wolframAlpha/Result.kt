package NoMathExpectation.NMEBoot.wolframAlpha

import kotlinx.serialization.Serializable

@Serializable
internal data class Result(
    val result: String? = null,
    val conversationID: String? = null,
    val host: String? = null,
    val s: String? = null,
    val error: String? = null
)
