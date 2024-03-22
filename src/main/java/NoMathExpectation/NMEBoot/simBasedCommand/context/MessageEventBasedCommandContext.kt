package NoMathExpectation.NMEBoot.simBasedCommand.context

import NoMathExpectation.NMEBoot.simBasedCommand.PermissionService
import love.forte.simbot.ability.SendSupport
import love.forte.simbot.definition.Actor
import love.forte.simbot.event.MessageEvent
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageContent
import net.mamoe.mirai.console.util.safeCast

interface MessageEventBasedCommandContext<out T : MessageEvent> : CommandContext<T> {
    override val origin: T

    override val globalSubject: Actor

    override val subject: Actor

    override val executor: Actor

    override suspend fun send(text: String) = subject.safeCast<SendSupport>()?.send(text)

    override suspend fun send(message: Message) = subject.safeCast<SendSupport>()?.send(message)

    override suspend fun send(messageContent: MessageContent) = subject.safeCast<SendSupport>()?.send(messageContent)

    override suspend fun reply(text: String) = origin.reply(text)

    override suspend fun reply(message: Message) = origin.reply(message)

    override suspend fun reply(messageContent: MessageContent) = origin.reply(messageContent)

    override suspend fun hasPermission(permission: String): Boolean {
        return PermissionService.hasPermission(permission, *permissionIds.toTypedArray())
    }

    override suspend fun setPermission(permission: String, value: Boolean?) {
        PermissionService.setPermission(permission, id, value)
    }
}