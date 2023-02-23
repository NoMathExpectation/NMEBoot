package NoMathExpectation.NMEBoot.inventory

import NoMathExpectation.NMEBoot.inventory.card.CardSerializer
import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.RecentActiveContact
import NoMathExpectation.NMEBoot.utils.getFriend
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.reloadAsJson
import kotlinx.serialization.Serializable
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.descriptor.CommandValueArgumentParser
import net.mamoe.mirai.console.command.descriptor.ExistingUserValueArgumentParser
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.MessageContent
import kotlin.math.min

@Serializable
class NormalUser private constructor(
    val id: Long,
    private val inventory: MutableMap<String, ItemStack<out Item>> = mutableMapOf() // id -> ItemStack
) {
    companion object : AutoSavePluginData("user") {
        override val serializersModule by ItemRegistry

        private val users by value<MutableMap<Long, NormalUser>>() // key: id, value: user

        init {
            CardSerializer.markAsLoaded()
            reload {
                reloadAsJson()
            }
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
        val itemStackInInventory = inventory.computeIfAbsent(item.id) { item count 0 }
        val count = if (item.negativeCountable) itemStack.count else min(itemStack.count, itemStackInInventory.count)
        if (count == 0) {
            return
        }

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

    fun discardItem(item: Item, overriddenOnDiscard: (NormalUser.() -> Unit)? = null) =
        discardItemStack(item count 1, overriddenOnDiscard)

    operator fun minusAssign(item: Item) = discardItem(item)

    operator fun Item.unaryMinus() = discardItem(this)

    operator fun contains(itemStack: ItemStack<out Item>) =
        itemStack.count <= 0 || (inventory[itemStack.item.id]?.count ?: 0) >= itemStack.count

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
    ): Boolean {
        if (itemStack.count < 1) {
            return false
        }

        val item = itemStack.item
        val itemStackInInventory = inventory.computeIfAbsent(item.id) { item count 0 }
        val count =
            if (item.negativeCountable || (item.reusable && itemStackInInventory.count > 0)) itemStack.count else min(
                itemStack.count,
                itemStackInInventory.count
            )

        if (count < 1) {
            return false
        }

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
        return true
    }

    @JvmBlockingBridge
    suspend fun useItem(item: Item, overriddenOnUse: (suspend NormalUser.() -> Boolean)? = null) =
        useItemStack(item count 1, overriddenOnUse)

    fun searchItem(predicate: (Item) -> Boolean = { true }) = inventory.values.filter { predicate(it.item) }

    fun searchItem(raw: String) = searchItem { it.id == raw || it.name.lowercase().contains(raw.lowercase()) }

    fun getInventory() = inventory.values.toList()
}

fun User.toNormalUser() = NormalUser[id]

fun CommandSender.toNormalUser() = user?.id?.let { NormalUser[it] }

fun NormalUser?.toString() = "User(${this?.let { "id: ${it.id}" } ?: "null"})"

fun NormalUser?.toFriend() = if (this == null) null else getFriend(id)

object NormalUserCommandParser : CommandValueArgumentParser<NormalUser> {
    override fun parse(raw: String, sender: CommandSender): NormalUser =
        ExistingUserValueArgumentParser.parse(raw, sender).toNormalUser().also {
            RecentActiveContact[it.id] = sender.subject
        }

    override fun parse(raw: MessageContent, sender: CommandSender) =
        ExistingUserValueArgumentParser.parse(raw, sender).toNormalUser().also {
            RecentActiveContact[it.id] = sender.subject
        }
}