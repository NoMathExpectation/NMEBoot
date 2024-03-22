package NoMathExpectation.NMEBoot.simBasedCommand.context

import love.forte.simbot.common.collectable.toList
import love.forte.simbot.component.kook.*
import love.forte.simbot.component.kook.event.KookChannelMessageEvent
import love.forte.simbot.component.kook.event.KookContactMessageEvent
import love.forte.simbot.component.kook.event.KookMessageEvent
import love.forte.simbot.component.kook.role.KookRole
import love.forte.simbot.definition.Actor

interface KookMessageEventBasedCommandContext<out T : KookMessageEvent> : MessageEventBasedCommandContext<T> {
    override val origin: T

    override val platform: String get() = "kook"

    override val globalSubject: Actor

    val globalSubjectAsKookGuild get() = globalSubject as? KookGuild

    val globalSubjectAsKookUserChat get() = globalSubject as? KookUserChat

    override val subject: Actor

    val subjectAsChannel get() = subject as? KookChatChannel

    val subjectAsUserChat get() = subject as? KookUserChat

    override val executor: Actor

    val executorAsChannel get() = executor as? KookChatChannel

    val executorAsUserChat get() = executor as? KookUserChat

    companion object {
        class MessageChannel(override val origin: KookChannelMessageEvent) :
            KookMessageEventBasedCommandContext<KookChannelMessageEvent> {
            override val id = origin.author.permissionId
            override val globalSubject = origin.source
            override val subject = origin.content
            override val executor = origin.author
            override val permissionIds =
                listOf(
                    origin.source.permissionId,
                    origin.content.permissionId,
                    *origin.author.rolePermissionIds.reversed().toTypedArray(),
                    origin.author.permissionId,
                )
        }

        class Contact(override val origin: KookContactMessageEvent) :
            KookMessageEventBasedCommandContext<KookContactMessageEvent> {
            override val id = origin.content.permissionId
            override val globalSubject = origin.content
            override val subject = origin.content
            override val executor = origin.content
            override val permissionIds = listOf(origin.content.permissionId)
        }
    }
}

val KookGuild.permissionId get() = "kook-guild-$id"

val KookChannel.permissionId get() = "kook-channel-$id"

val KookMember.permissionId get() = "kook-member-$id"

val KookUserChat.permissionId get() = "kook-userChat-$id"

val KookRole.permissionId get() = "kook-role-$id"

val KookMember.rolePermissionIds get() = roles.toList().map { it.permissionId }