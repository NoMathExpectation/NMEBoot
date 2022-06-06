package NoMathExpectation.NMEBoot.RDLounge.rhythmCafe

import kotlinx.serialization.Serializable

@Serializable
internal data class RequestedParameter(val collection_name: String,
                                       val per_page: Int,
                                       val q: String)
