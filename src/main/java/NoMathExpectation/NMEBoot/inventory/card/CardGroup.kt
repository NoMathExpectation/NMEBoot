package NoMathExpectation.NMEBoot.inventory.card.rdTradingCards

import NoMathExpectation.NMEBoot.inventory.Pool
import NoMathExpectation.NMEBoot.inventory.PoolAsListSerializer
import NoMathExpectation.NMEBoot.inventory.PullStrategy
import NoMathExpectation.NMEBoot.inventory.card.Card
import NoMathExpectation.NMEBoot.inventory.card.CardRepository
import NoMathExpectation.NMEBoot.inventory.weightedPullStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder

@Serializable
data class CardGroup(
    @SerialName("groupname") val name: String,
    @SerialName("basechance") val chance: Double,
    @Serializable(CardGroupPoolSerializer::class) val cards: Pool<Card>
) {
    operator fun get(index: Int) = cards[index]

    fun pull(strategy: PullStrategy<Card> = cards.pullStrategy) = cards.pull(strategy)

    fun setRepository(repository: CardRepository) = cards.forEach { it.repository = repository }
}

class CardGroupPoolSerializer : PoolAsListSerializer<Card>(ListSerializer(Card.serializer())) {
    override fun deserialize(decoder: Decoder) =
        super.deserialize(decoder).apply { pullStrategy = weightedPullStrategy { chance } }
}