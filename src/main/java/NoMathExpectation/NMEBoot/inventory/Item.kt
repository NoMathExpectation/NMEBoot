package NoMathExpectation.NMEBoot.inventory

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.descriptor.CommandArgumentParserException
import net.mamoe.mirai.console.command.descriptor.CommandValueArgumentParser
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.buildMessageChain

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("item_class")
interface Item {
    val id: String
    val name: String
    val description: String

    // whether user can hold negative count of this item
    val negativeCountable get() = false

    // whether item can be used multiple times
    val reusable get() = false

    fun NormalUser.onGive() {}

    fun NormalUser.onDiscard() {}

    suspend fun NormalUser.onUse() = false

    fun show() = buildMessageChain {
        appendLine("名称： $name")
        appendLine("id： $id")
        appendLine("描述：")
        appendLine(description)
    }

    fun showSimple(showId: Boolean = false) = buildString {
        append(name)
        if (showId) append(" (id: $id)")
    }

    infix fun count(count: Int) = ItemStack(this, count)
}

fun Item?.toString() = "Item(${this?.let { "id: ${it.id}, name: ${it.name}" } ?: "null"})"

fun Item?.equals(other: Item?) = this?.id == other?.id

fun MessageChainBuilder.add(item: Item) = apply { +item.showSimple() }


object ItemRegistry {
    @JvmName("getSerializersModule")
    operator fun getValue(thisRef: Any?, property: Any?) = SerializersModule {
        polymorphic(Item::class) {
            registerFunctions.forEach { it() }
        }
    }

    private val itemMap: MutableMap<String, Item> = mutableMapOf()

    private val registerFunctions: MutableList<PolymorphicModuleBuilder<Item>.() -> Unit> = mutableListOf()


    operator fun plusAssign(registerFunction: PolymorphicModuleBuilder<Item>.() -> Unit) {
        registerFunctions += registerFunction
    }

    operator fun plusAssign(item: Item) {
        itemMap[item.id] = item
    }

    operator fun get(id: String) = itemMap[id]
}

inline fun <reified I : Item> registerItemClass() {
    ItemRegistry += { subclass(I::class) }
}

inline fun <reified I : Item> registerItem(item: I) {
    ItemRegistry += item
    registerItemClass<I>()
}


object ItemCommandParser : CommandValueArgumentParser<Item> {
    override fun parse(raw: String, sender: CommandSender) =
        ItemRegistry[raw] ?: throw CommandArgumentParserException("找不到物品 $raw")
}