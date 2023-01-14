package NoMathExpectation.NMEBoot.RDLounge.NeoCardSystem.rdTradingCards

import NoMathExpectation.NMEBoot.RDLounge.NeoCardSystem.Card
import NoMathExpectation.NMEBoot.inventory.Pool
import NoMathExpectation.NMEBoot.inventory.PoolAsListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardGroup(
    @SerialName("groupname") val name: String,
    @SerialName("basechance") val chance: Double,
    @Serializable(PoolAsListSerializer::class) val cards: Pool<Card>
) {
    operator fun get(index: Int) = cards[index]
}