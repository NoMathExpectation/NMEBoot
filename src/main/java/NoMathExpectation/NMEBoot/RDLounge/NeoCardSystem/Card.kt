package NoMathExpectation.NMEBoot.RDLounge.NeoCardSystem

import NoMathExpectation.NMEBoot.RDLounge.NeoCardSystem.rdTradingCards.CardLibrary
import NoMathExpectation.NMEBoot.inventory.Item
import NoMathExpectation.NMEBoot.inventory.NormalUser
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.sendMessage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(CardSerializer::class)
@SerialName("rdcard")
data class Card(
    val filename: String,
    override val name: String,
    val type: String,
    override val description: String,
    val chance: Double,
    val smell: String? = null,
    val embed: String? = null
) : Item {
    override val id get() = "rdcard:$filename"
    override val reusable get() = true

    override suspend fun NormalUser.onUse(): Boolean {
        if (smell == null) return false

        sendMessage("你闻了闻 $name ，一股 $smell 的味道")
        return true
    }
}

@Serializable
@SerialName("rdcard")
private data class CardSurrogate(
    val filename: String,
    val name: String,
    val type: String,
    val description: String,
    val chance: Double,
    val smell: String? = null,
    val embed: String? = null
) {
    val id get() = "rdcard:$filename"

    fun toReal() = Card(filename, name, type, description, chance, smell, embed)
}

private fun Card.toSurrogate() = CardSurrogate(filename, name, type, description, chance, smell, embed)

private object CardSerializer : KSerializer<Card> {
    override val descriptor: SerialDescriptor = CardSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Card) =
        encoder.encodeSerializableValue(CardSurrogate.serializer(), value.toSurrogate())

    override fun deserialize(decoder: Decoder): Card {
        val surrogate = decoder.decodeSerializableValue(CardSurrogate.serializer())
        return CardLibrary.library[surrogate.id] ?: run {
            logger.warning("rd卡牌库：读取数据时发现未知卡牌：$surrogate ，将使用来自此卡牌的数据")
            surrogate.toReal()
        }
    }
}