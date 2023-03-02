package NoMathExpectation.NMEBoot.utils

import NoMathExpectation.NMEBoot.Main
import NoMathExpectation.NMEBoot.commandSystem.services.RDLoungeIntegrated
import NoMathExpectation.NMEBoot.utils.MessageHistory.randomAsMessage
import kotlinx.datetime.Clock
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildMessageChain
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object MessageHistory {
    private fun record(event: MessageEvent) = transaction {
        MessageHistoryTable.insert {
            it[sender] = event.sender.id
            it[group] = when (event) {
                is GroupMessageEvent -> event.group.id
                is GroupMessageSyncEvent -> event.group.id
                else -> null
            }
            it[name] = event.senderName
            it[message] = event.message.serializeToMiraiCode()
            it[time] = Clock.System.now().toEpochMilliseconds()
        }
    }

    private fun record(event: MessagePostSendEvent<out Contact>) {
        if (event.isFailure) {
            return
        }

        transaction {
            MessageHistoryTable.insert {
                it[sender] = event.bot.id
                it[group] = when (event) {
                    is GroupMessagePostSendEvent -> event.target.id
                    else -> null
                }
                it[name] = when (event) {
                    is GroupMessagePostSendEvent -> event.target.botAsMember.nameCardOrNick
                    is FriendMessagePostSendEvent -> event.bot.nameCardOrNick
                    is GroupTempMessagePostSendEvent -> event.group.botAsMember.nameCardOrNick
                    is StrangerMessagePostSendEvent -> event.bot.nameCardOrNick
                }
                it[message] = event.message.serializeToMiraiCode()
                it[time] = Clock.System.now().toEpochMilliseconds()
            }
        }
    }

    fun recordStart() {
        val channel = GlobalEventChannel.parentScope(Main.INSTANCE)
        channel.subscribeAlways<MessageEvent>(priority = EventPriority.MONITOR) {
            record(it)
        }
        channel.subscribeAlways<MessagePostSendEvent<out Contact>>(priority = EventPriority.MONITOR) {
            record(it)
        }
    }

    fun random(group: Long? = null, bot: Long? = null) = transaction {
        var query =
            MessageHistoryTable.select { ((MessageHistoryTable.message notLike "//%") and (MessageHistoryTable.message neq "")) }
        group?.let {
            query = query.andWhere { MessageHistoryTable.group eq it }
        }
        bot?.let {
            query = query.andWhere { MessageHistoryTable.sender neq it }
        }
        query.orderBy(Random()).limit(1).firstOrNull()
    }

    fun randomAsMessage(group: Long? = null, bot: Long? = null): Pair<MessageChain, MessageChain>? {
        val message = random(group, bot) ?: return null
        val msg1 = buildMessageChain {
            +"${message[MessageHistoryTable.name]} 曾经说过："
        }
        val msg2 = message[MessageHistoryTable.message].deserializeMiraiCode()
        return msg1 to msg2
    }
}

object MessageHistoryTable : LongIdTable() {
    val sender = long("sender")
    val group = long("group").nullable()
    val name = varchar("name", 127)
    val message = text("message")
    val time = long("time")
}

internal fun nudgeForRandomMessage() {
    botEventChannel.subscribeAlways<NudgeEvent> { e ->
        val target = e.from.id
        val to = e.target.id
        if (to != e.bot.id || to == target || target == e.bot.id) {
            return@subscribeAlways
        }

        val from = e.subject
        if (from.id != RDLoungeIntegrated.RDLOUNGE) {
            randomAsMessage(from.id, e.bot.id)?.second?.let { from.sendMessage(it) }
        }
    }
}