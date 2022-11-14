package NoMathExpectation.NMEBoot.RDLounge.rhythmCafe

import kotlinx.serialization.Serializable

@Serializable
internal data class Result(
    val facet_counts: List<Filter>,
    val found: Int,
    val hits: List<MatchedLevel>,
    val out_of: Int,
    val page: Int,
    val request_params: RequestedParameter,
    val search_cutoff: Boolean,
    val search_time_ms: Long
)
