package NoMathExpectation.NMEBoot.simBasedCommand

import NoMathExpectation.NMEBoot.simbot.SimConfig
import NoMathExpectation.NMEBoot.utils.logger
import love.forte.simbot.ability.StandardDeleteOption
import love.forte.simbot.common.collectable.toList
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.component.kook.KookMember
import love.forte.simbot.definition.Actor
import love.forte.simbot.definition.Organization
import love.forte.simbot.event.ActorAuthorAwareMessageEvent
import love.forte.simbot.event.ActorEvent
import love.forte.simbot.event.MessageEvent

suspend fun MessageEvent.subject(): Actor? = if (this is ActorEvent) content() else null

suspend fun MessageEvent.sender(): Actor? = if (this is ActorAuthorAwareMessageEvent) author() else null

fun Actor.roles() = when (this) {
    is Organization -> roles.toList().toSet()
    is KookMember -> roles.toList().toSet()
    else -> emptySet()
}

fun Actor.hasRole(roleId: String) = roleId.ID in roles().map { it.id }

fun Actor.giveRole(roleId: String) = when (this) {
    is KookMember -> kotlin.runCatching {
        SimConfig.kookBot
            ?.guildRelation
            ?.getGuild(guildId)
            ?.roles
            ?.toList()
            ?.firstOrNull { it.id == roleId.ID }
            ?.grantToBlocking(this) ?: return@runCatching false
        true
    }.onFailure {
        logger.error(it)
    }.getOrDefault(false)

    else -> false
}

fun Actor.revokeRole(roleId: String) = when (this) {
    is KookMember -> kotlin.runCatching {
        roles.toList()
            .firstOrNull { it.id == roleId.ID }
            ?.deleteBlocking(StandardDeleteOption.IGNORE_ON_NO_SUCH_TARGET) ?: return@runCatching false
        true
    }.onFailure {
        logger.error(it)
    }.getOrDefault(false)

    else -> false
}