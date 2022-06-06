package NoMathExpectation.NMEBoot.RDLounge.rhythmCafe

import kotlinx.serialization.Serializable

@Serializable
internal data class FilterStat(val total_values: Int,
                               val avg: Double? = null,
                               val max: Double? = null,
                               val min: Double? = null,
                               val sum: Double? = null)
