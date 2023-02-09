package NoMathExpectation.NMEBoot.inventory.card

import NoMathExpectation.NMEBoot.inventory.Item
import NoMathExpectation.NMEBoot.inventory.NormalUser
import NoMathExpectation.NMEBoot.utils.ktorClient
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.sendMessage
import NoMathExpectation.NMEBoot.utils.toExternalResource
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.utils.io.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource

@Serializable(CardSerializer::class)
@SerialName("rdcard")
data class Card(
    val filename: String,
    override val name: String,
    val type: String,
    override val description: String,
    val chance: Double,
    val smell: String? = null,
    val embed: String? = null,
    @Transient var repository: CardRepository? = null
) : Item {
    override val id get() = "rdcard:$filename"
    override val reusable get() = true

    override suspend fun NormalUser.onUse(): Boolean {
        if (smell == null) return false

        sendMessage("你闻了闻 $name ，一股 $smell 的味道")
        return true
    }

    suspend fun getImage(contact: Contact): Image {
        // by local asset
        if (repository != null) {
            repository!!.filenameToFileMap[filename]?.let { file ->
                try {
                    file.toExternalResource().use {
                        return contact.uploadImage(it)
                    }
                } catch (e: Exception) {
                    logger.debug(e)
                }
            }
        }

        // by embed url
        if (embed != null) {
            try {
                return ktorClient.get(embed)
                    .body<ByteReadChannel>()
                    .toExternalResource()
                    .use { contact.uploadImage(it) }
            } catch (e: Exception) {
                logger.debug(e)
            }
        }

        logger.warning("卡牌库：无法获取 ${repository?.gitRepository ?: "不在"} 库中的卡片 $name ($id) 的图片，请检查文件")

        // default error png in jar
        javaClass.getResourceAsStream("item/card/error.png")
            ?.let { res ->
                try {
                    return res.toExternalResource()
                        .use { contact.uploadImage(it) }
                } catch (e: Exception) {
                    logger.debug(e)
                }
            }

        // default image on server, in case of png in jar is missing
        return Image("3008D1A5-1017-50DD-F60B-B21A620ADCB9")
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
        return CardRepository[surrogate.id] ?: run {
            logger.warning("rd卡牌库：读取数据时发现未知卡牌：$surrogate ，将使用来自此卡牌的数据")
            surrogate.toReal()
        }
    }
}