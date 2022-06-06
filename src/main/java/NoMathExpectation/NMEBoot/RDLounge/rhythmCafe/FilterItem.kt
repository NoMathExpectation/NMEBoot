package NoMathExpectation.NMEBoot.RDLounge.rhythmCafe

import kotlinx.serialization.Serializable

@Serializable
internal data class FilterItem(val count: Int,
                               val highlighted: String,
                               val value: String)
