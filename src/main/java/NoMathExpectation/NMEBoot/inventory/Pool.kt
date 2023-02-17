package NoMathExpectation.NMEBoot.inventory

import NoMathExpectation.NMEBoot.inventory.card.Card
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.reloadAsJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

typealias PullStrategy<E> = (List<E>) -> E?

@Serializable
class Pool<E>(
    private val content: MutableList<E> = mutableListOf(),
    val exhaustible: Boolean = true,
    @Transient var pullStrategy: PullStrategy<E> = uniformPullStrategy()
) : MutableList<E> by content {
    override fun add(element: E): Boolean {
        if (!exhaustible && content.contains(element)) return false
        return content.add(element)
    }

    override fun add(index: Int, element: E) {
        if (!exhaustible && content.contains(element)) return
        content.add(index, element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        if (!exhaustible) {
            val newElements = elements.filter { !content.contains(it) }
            return content.addAll(newElements)
        }
        return content.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        if (!exhaustible) {
            val newElements = elements.filter { !content.contains(it) }
            return content.addAll(index, newElements)
        }
        return content.addAll(index, elements)
    }

    fun pull(strategy: PullStrategy<E> = pullStrategy): E? = strategy(content).also {
        if (exhaustible) content.remove(it)
        pullCount++
        logger.debug("Pulled $it from $this.")
    }

    companion object : AutoSavePluginData("pool") {
        override val serializersModule by ItemRegistry

        val ground: Pool<Item> by value()
        val box: Pool<Card> by value()
        var pullCount by value(0)
            private set

        init {
            reloadAsJson()
        }
    }
}

fun <E> uniformPullStrategy() = { list: List<E> -> list.randomOrNull() }

fun <E> weightedPullStrategy(provider: E.() -> Double) = fun(list0: List<E>): E? {
    if (list0.isEmpty()) {
        return null
    }

    val list = list0.filter { it.provider() > 0 }

    val totalWeight = list.sumOf { it.provider() }
    val randomWeight = Math.random() * totalWeight
    var currentWeight = 0.0
    for (item in list) {
        currentWeight += item.provider()
        if (currentWeight >= randomWeight) return item
    }
    return list.last()
}

open class PoolAsListSerializer<E>(private val listKSerializer: KSerializer<List<E>>) : KSerializer<Pool<E>> {
    override val descriptor: SerialDescriptor = listKSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Pool<E>) = listKSerializer.serialize(encoder, value)

    override fun deserialize(decoder: Decoder) = Pool(listKSerializer.deserialize(decoder).toMutableList())
}