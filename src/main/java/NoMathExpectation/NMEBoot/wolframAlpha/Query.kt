package NoMathExpectation.NMEBoot.wolframAlpha

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/v1/conversation.jsp")
internal data class Query(
    val appid: String,
    val i: String,
    val conversationid: String? = null,
    val geolocation: String? = null,
    val ip: String? = null,
    val units: String? = null,
    val s: String? = null
)
