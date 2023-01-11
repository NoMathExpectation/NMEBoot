package NoMathExpectation.NMEBoot.inventory

import NoMathExpectation.NMEBoot.utils.getFriend
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.plugin
import kotlinx.serialization.Serializable
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginData
import net.mamoe.mirai.contact.User
import kotlin.math.min

@Serializable
class NormalUser private constructor(
    val id: Long,
    private val inventory: MutableMap<String, ItemStack<out Item>> = mutableMapOf() // id -> ItemStack
) {
    companion object : AutoSavePluginData("user") {
        override val serializersModule by ItemSerializerModuleProvider

        private val users by value<MutableMap<Long, NormalUser>>() // key: id, value: user

        init {
            plugin.reloadPluginData(this)
        }

        @JvmStatic
        operator fun get(id: Long) = users.getOrPut(id) { NormalUser(id) }
    }

    fun giveItemStack(itemStack: ItemStack<out Item>, overriddenOnGive: (NormalUser.() -> Unit)? = null) {
        if (itemStack.count == 0) {
            return
        }

        if (itemStack.count < 0) {
            discardItemStack(itemStack * -1, overriddenOnGive)
            return
        }

        val item = itemStack.item
        val count = itemStack.count
        val itemStackInInventory = inventory.computeIfAbsent(item.id) { item count 0 }
        itemStackInInventory += count
        logger.info("物品系统：用户 $id 获得了 ${itemStack.count} 个物品 ${item.showSimple(true)}，现在拥有 ${itemStackInInventory.count} 个物品")
        repeat(count) {
            if (overriddenOnGive != null) overriddenOnGive()
            else with(item) { onGive() }
        }
    }

    operator fun plusAssign(itemStack: ItemStack<out Item>) = giveItemStack(itemStack)

    operator fun ItemStack<out Item>.unaryPlus() = giveItemStack(this)

    fun giveItem(item: Item, overriddenOnGive: (NormalUser.() -> Unit)? = null) =
        giveItemStack(item count 1, overriddenOnGive)

    operator fun plusAssign(item: Item) = giveItem(item)

    operator fun Item.unaryPlus() = giveItem(this)

    fun discardItemStack(itemStack: ItemStack<out Item>, overriddenOnDiscard: (NormalUser.() -> Unit)? = null) {
        if (itemStack.count == 0) {
            return
        }

        if (itemStack.count < 0) {
            giveItemStack(itemStack * -1, overriddenOnDiscard)
            return
        }

        val item = itemStack.item
        val count = if (item.negativeCountable) itemStack.count else min(itemStack.count, itemStack.count)
        val itemStackInInventory = inventory.computeIfAbsent(item.id) { item count 0 }

        itemStackInInventory -= count
        if (itemStackInInventory.count == 0) {
            inventory.remove(itemStackInInventory.item.id)
        }
        logger.info("物品系统：用户 $id 丢弃了 ${itemStack.count} 个物品 ${item.showSimple(true)}，还剩下 ${itemStackInInventory.count} 个物品")

        repeat(count) {
            if (overriddenOnDiscard != null) overriddenOnDiscard()
            else with(item) { onDiscard() }
        }
    }

    operator fun minusAssign(itemStack: ItemStack<out Item>) = discardItemStack(itemStack)

    operator fun ItemStack<out Item>.unaryMinus() = discardItemStack(this)

    fun discardItem(item: Item, overriddenOnGive: (NormalUser.() -> Unit)? = null) =
        discardItemStack(item count 1, overriddenOnGive)

    operator fun minusAssign(item: Item) = discardItem(item)

    operator fun Item.unaryMinus() = discardItem(this)

    operator fun contains(itemStack: ItemStack<out Item>) =
        (inventory[itemStack.item.id]?.count ?: 0) >= itemStack.count

    operator fun contains(item: Item) = item count 1 in this

    fun tryAndDiscardItemStack(itemStack: ItemStack<out Item>, overriddenOnDiscard: (NormalUser.() -> Unit)? = null) =
        (itemStack in this).also {
            if (it) {
                discardItemStack(itemStack, overriddenOnDiscard)
            }
        }

    fun tryAndDiscardItem(item: Item, overriddenOnDiscard: (NormalUser.() -> Unit)? = null) =
        tryAndDiscardItemStack(item count 1, overriddenOnDiscard)

    @JvmBlockingBridge
    suspend fun useItemStack(
        itemStack: ItemStack<out Item>,
        overriddenOnUse: (suspend NormalUser.() -> Boolean)? = null
    ) {
        if (itemStack.count <= 0) {
            return
        }

        val item = itemStack.item
        val itemStackInInventory = inventory.computeIfAbsent(item.id) { item count 0 }
        val count =
            if (item.negativeCountable || (item.reusable && itemStackInInventory.count > 0)) itemStack.count else min(
                itemStack.count,
                itemStack.count
            )

        if (!item.reusable) {
            itemStackInInventory -= count
            if (itemStackInInventory.count == 0) {
                inventory.remove(itemStackInInventory.item.id)
            }
        }
        logger.info("物品系统：用户 $id 使用了 ${itemStack.count} 个物品 ${item.showSimple(true)}，还剩下 ${itemStackInInventory.count} 个物品")

        repeat(count) {
            if (overriddenOnUse != null) overriddenOnUse()
            else with(item) { onUse() }
        }
    }

    @JvmBlockingBridge
    suspend fun useItem(item: Item, overriddenOnUse: (suspend NormalUser.() -> Boolean)? = null) =
        useItemStack(item count 1, overriddenOnUse)
}

fun User.toNormalUser() = NormalUser[this.id]

fun NormalUser?.toString() = "User(${this?.let { "id: ${it.id}" } ?: "null"})"

fun NormalUser?.toFriend() = if (this == null) null else getFriend(id)