package NoMathExpectation.NMEBoot.wolframAlpha

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/v1/conversation.jsp")
internal data class Query(
    val appid: String,
    val i: String,
    val conversationid: String? = null,
    val geolocation: String? = "0.0,0.0",
    val ip: String? = "255.255.255.255",
    val units: String? = "metric",
    val s: String? = null
)
