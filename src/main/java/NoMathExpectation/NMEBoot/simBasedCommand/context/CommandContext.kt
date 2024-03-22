package NoMathExpectation.NMEBoot.simBasedCommand.context

import love.forte.simbot.definition.Actor
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageContent
import love.forte.simbot.message.MessageReceipt
import love.forte.simbot.message.toText
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

interface CommandContext<out T> {
    val origin: T

    val id: String

    val permissionIds: List<String>

    val platform: String

    val globalSubject: Actor?

    val subject: Actor?

    val executor: Actor?

    suspend fun send(text: String): MessageReceipt? = send(text.toText())

    suspend fun send(message: Message): MessageReceipt?

    suspend fun send(messageContent: MessageContent): MessageReceipt? = send(messageContent.messages)

    suspend fun reply(text: String): MessageReceipt? = reply(text.toText())

    suspend fun reply(message: Message): MessageReceipt?

    suspend fun reply(messageContent: MessageContent): MessageReceipt? = reply(messageContent.messages)

    suspend fun hasPermission(permission: String): Boolean

    suspend fun setPermission(permission: String, value: Boolean?)

    companion object {
        fun interface CommandContextBuilder<T : Any> {
            fun build(origin: T): CommandContext<T>

            @Suppress("UNCHECKED_CAST")
            fun buildFromAny(origin: Any) = (origin as? T)?.let { build(it) }
        }

        private val registry: MutableMap<KClass<*>, CommandContextBuilder<*>> = mutableMapOf()

        fun <T : Any> register(clazz: KClass<T>, builder: CommandContextBuilder<T>) {
            registry[clazz] = builder
        }

        inline fun <reified T : Any> register(noinline builder: (T) -> CommandContext<T>) {
            register(T::class, builder)
        }

        operator fun get(origin: Any): CommandContext<*>? {
            var classes = listOf(origin::class)
            while (classes.isNotEmpty()) {
                classes.firstNotNullOfOrNull {
                    registry[it]?.buildFromAny(origin)
                }?.let { return it }
                classes = classes.flatMap { it.superclasses }
            }
            return null
        }
    }
}