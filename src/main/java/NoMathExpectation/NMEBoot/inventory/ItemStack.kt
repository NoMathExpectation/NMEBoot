package NoMathExpectation.NMEBoot.inventory

import kotlinx.serialization.Serializable
import net.mamoe.mirai.message.data.MessageChainBuilder

@Serializable
data class ItemStack<I : Item>(val item: I, var count: Int = 1) {
    operator fun plus(other: ItemStack<I>) = ItemStack(item, count + other.count)

    operator fun plus(count: Int) = ItemStack(item, this.count + count)

    operator fun plusAssign(other: ItemStack<I>) {
        count += other.count
    }

    operator fun plusAssign(count: Int) {
        this.count += count
    }

    operator fun minus(other: ItemStack<I>) = ItemStack(item, count - other.count)

    operator fun minus(count: Int) = ItemStack(item, this.count - count)

    operator fun minusAssign(other: ItemStack<I>) {
        count -= other.count
    }

    operator fun minusAssign(count: Int) {
        this.count -= count
    }

    operator fun times(other: ItemStack<I>) = ItemStack(item, count * other.count)

    operator fun times(count: Int) = ItemStack(item, this.count * count)

    operator fun timesAssign(other: ItemStack<I>) {
        count *= other.count
    }

    operator fun timesAssign(count: Int) {
        this.count *= count
    }

    operator fun div(other: ItemStack<I>) = ItemStack(item, count / other.count)

    operator fun div(count: Int) = ItemStack(item, this.count / count)

    operator fun divAssign(other: ItemStack<I>) {
        count /= other.count
    }

    operator fun divAssign(count: Int) {
        this.count /= count
    }

    operator fun compareTo(other: ItemStack<I>) = count.compareTo(other.count)

    fun showSimple(showId: Boolean = false) = "${count}x ${item.showSimple(showId)}"
}

fun MessageChainBuilder.add(itemStack: ItemStack<*>) = apply { +itemStack.showSimple() }