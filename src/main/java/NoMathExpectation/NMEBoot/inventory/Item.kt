package NoMathExpectation.NMEBoot.inventory

import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.buildMessageChain

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

object ItemSerializerModuleProvider {
    private var serializersModule = getModule()

    @JvmName("get")
    operator fun getValue(thisRef: Any?, property: Any?) = serializersModule

    private val registerFunctions: MutableList<PolymorphicModuleBuilder<Item>.() -> Unit> = mutableListOf()

    @JvmName("register")
    operator fun plusAssign(registerFunction: PolymorphicModuleBuilder<Item>.() -> Unit) {
        registerFunctions += registerFunction
    }

    private fun getModule() = SerializersModule {
        polymorphic(Item::class) {
            registerFunctions.forEach { it() }
        }
    }

    fun reload() {
        serializersModule = getModule()
    }
}

@Suppress("UnusedReceiverParameter")
inline fun <reified I : Item> I.register() {
    ItemSerializerModuleProvider += { subclass(I::class) }
}