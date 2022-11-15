package NoMathExpectation.NMEBoot.utils

import kotlinx.datetime.Clock
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupMessageSyncEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildMessageChain
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Random
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
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

    fun recordStart() = GlobalEventChannel.subscribeAlways<MessageEvent> {
        record(it)
    }

    fun random(group: Long) = transaction {
        MessageHistoryTable.select { (MessageHistoryTable.group eq group) and (MessageHistoryTable.message notLike "//history%") and (MessageHistoryTable.message neq "") }
            .orderBy(Random()).limit(1).first()
    }

    fun randomAsMessage(group: Long): Pair<MessageChain, MessageChain> {
        val message = random(group)
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