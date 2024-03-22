package NoMathExpectation.NMEBoot.utils

import NoMathExpectation.NMEBoot.commands.CommandStats
import NoMathExpectation.NMEBoot.sending.FoldIgnore
import NoMathExpectation.NMEBoot.utils.MessageHistory.randomAsMessage
import com.seaboat.text.analyzer.segment.DictSegment
import com.seaboat.text.analyzer.segment.Segment
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import net.mamoe.mirai.console.command.CommandSender.Companion.asCommandSender
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.contact.roaming.RoamingSupported
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.MiraiInternalApi
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

object MessageHistory : AutoSavePluginConfig("message_history") {
    private fun record(event: MessageEvent) = transaction {
        MessageHistoryTable.insert {
            it[ids] = event.message.sourceOrNull?.ids?.joinToString() ?: ""
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
                it[ids] = event.message.sourceOrNull?.ids?.joinToString() ?: ""
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

    private fun record(event: Event) = transaction {
        EventHistoryTable.insert {
            it[type] = event::class.qualifiedName ?: "<anonymous class>"
            it[detail] = event.toString()
            it[time] = Clock.System.now().toEpochMilliseconds()
        }
    }

    fun recordStart() {
        val channel = GlobalEventChannel.parentScope(plugin)
        channel.subscribeAlways<MessageEvent>(priority = EventPriority.MONITOR) {
            record(it)
        }
        channel.subscribeAlways<MessagePostSendEvent<out Contact>>(priority = EventPriority.MONITOR) {
            record(it)
        }

        channel.subscribeAlways<Event>(priority = EventPriority.MONITOR) {
            record(it)
        }
    }

    fun random(group: Long? = null, bot: Long? = null, count: Int) = transaction {
        var query =
            MessageHistoryTable.select { ((MessageHistoryTable.message notLike "//%") and (MessageHistoryTable.message neq "")) }
        group?.let {
            query = query.andWhere { MessageHistoryTable.group eq it }
        }
        bot?.let {
            query = query.andWhere { MessageHistoryTable.sender neq it }
        }
        query.orderBy(SQLRandom()).limit(count)
    }

    fun random(group: Long? = null, bot: Long? = null) = transaction {
        random(group, bot, 1).firstOrNull()
    }

    fun randomAsMessage(group: Long? = null, bot: Long? = null): Pair<MessageChain, MessageChain>? {
        val message = random(group, bot) ?: return null
        val msg1 = buildMessageChain {
            +"${message[MessageHistoryTable.name]} 曾经说过："
        }
        val msg2 = FoldIgnore + message[MessageHistoryTable.message].deserializeMiraiCode()
        return msg1 to msg2
    }

    private val segmenter: Segment = DictSegment.get()
    fun randomGarbage(
        group: Long? = null,
        bot: Long? = null,
        count: Int = 10,
        filter: Regex = "[\\p{Punct}\\w]+".toRegex()
    ) = buildString {
        val wordlist = transaction {
            MessageHistory.random(group, bot, count)
                .asSequence()
                .flatMap { segmenter.seg(it[MessageHistoryTable.message])!! }
                .map { it.replaceAfterLast('/', "").replace(filter, "") }
                .toMutableList()
        }

        repeat(count) {
            append(wordlist.removeAt(Random.nextInt(wordlist.size)))
        }
    }

    init {
        transaction {
            """
            create view if not exists DailyMessageSend as
            select "group", sender, date(time / 1000, 'unixepoch', 'localtime') as date, count(*) as count
            from MessageHistory
            group by "group", sender, date(time / 1000, 'unixepoch', 'localtime');
            """.trimIndent().execAndMap { }

            """
            create view if not exists MostMessageSend as
            select "group", sender, count(*) as count
            from MessageHistory
            group by "group", sender;
            """.trimIndent().execAndMap { }
        }

        CommandStats.appendStats {
            transaction {
                (it as? Group)?.let {
                    +"群日消息总数："
                    """
                    select sum(count) as sum
                    from DailyMessageSend
                    where "group" = ${it.id}
                    and date = date('now', 'localtime');
                    """.trimIndent()
                        .execAndMap { it.getLong("sum") }
                        .firstOrNull()
                        ?.let { +it.toString() } ?: +"N/A"
                    +"\n"

                    +"群消息总数："
                    +MessageHistoryTable.select { MessageHistoryTable.group eq it.id }.count().toString()
                    +"\n"
                }

                +"日消息总数："
                """
                select sum(count) as sum
                from DailyMessageSend
                where date = date('now', 'localtime');
                """.trimIndent()
                    .execAndMap { it.getLong("sum") }
                    .firstOrNull()
                    ?.let { +it.toString() } ?: +"N/A"
                +"\n"

                +"消息总数："
                +MessageHistoryTable.selectAll().count().toString()
            }
        }

        CommandStats.appendStats("chat_daily", "群日消息排名") {
            transaction {
                if (it !is Group) {
                    +"只有群聊才可以使用此数据"
                    return@transaction
                }

                +"群日消息排名：\n"

                """
                select sender, count
                from DailyMessageSend
                where "group" = ${it.id}
                  and date = date('now', 'localtime')
                order by count desc;
                """.trimIndent().execAndMap { rs ->
                    rs.getLong("sender").let { sender -> it[sender]?.nameCardOrNick ?: "$sender" } to rs.getInt("count")
                }.forEachIndexed { index, (first, second) ->
                    +"${index + 1}. $first $second 条消息\n"
                }
            }
        }

        CommandStats.appendStats("chat_yesterday", "群昨日消息排名") {
            transaction {
                if (it !is Group) {
                    +"只有群聊才可以使用此数据"
                    return@transaction
                }

                +"群昨日消息排名：\n"

                """
                select sender, count
                from DailyMessageSend
                where "group" = ${it.id}
                  and date = date('now', 'localtime', '-1 day')
                order by count desc;
                """.trimIndent().execAndMap { rs ->
                    rs.getLong("sender").let { sender -> it[sender]?.nameCardOrNick ?: "$sender" } to rs.getInt("count")
                }.forEachIndexed { index, (first, second) ->
                    +"${index + 1}. $first $second 条消息\n"
                }
            }
        }

        CommandStats.appendStats("chat", "群总消息排名") {
            transaction {
                if (it !is Group) {
                    +"只有群聊才可以使用此数据"
                    return@transaction
                }

                +"群总消息排名：\n"

                """
                select sender, count
                from MostMessageSend
                where "group" = ${it.id}
                order by count desc;
                """.trimIndent().execAndMap { rs ->
                    rs.getLong("sender").let { sender -> it[sender]?.nameCardOrNick ?: "$sender" } to rs.getInt("count")
                }.forEachIndexed { index, (first, second) ->
                    +"${index + 1}. $first $second 条消息\n"
                }
            }
        }
    }
}

object MessageHistoryTable : LongIdTable() {
    val ids = text("ids").default("")
    val sender = long("sender")
    val group = long("group").nullable()
    val name = varchar("name", 127)
    val message = text("message")
    val time = long("time")
}

object EventHistoryTable : LongIdTable() {
    val type = text("type")
    val detail = text("detail")
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
        val commandSender = when (e.from) {
            is Friend -> (e.from as Friend).asCommandSender()
            is Member -> (e.from as Member).asCommandSender(false)
            else -> null
        }
        if (commandSender?.hasUsePermission() != true) {
            return@subscribeAlways
        }

        randomAsMessage(from.id, e.bot.id)?.second?.let { from.sendMessage(it) }
    }
}

fun MessageSource.roamingBlocking(contact: Contact?) = runBlocking { roaming(contact) }

//@jvmblockingbridge
suspend fun MessageSource.roaming(contact: Contact?): MessageChain? {
//    val subject = when (this) {
//        is OnlineMessageSource.Incoming -> fromId
//        is OnlineMessageSource.Outgoing -> targetId
//        is OfflineMessageSource -> if (botId == fromId) targetId else fromId
//        else -> return null
//    }

    val id = contact?.id ?: return null
    val idsString = ids.joinToString()
    @OptIn(MiraiInternalApi::class)
    transaction {
        when (contact) {
            is Friend, is Member, is TempUser, is Stranger -> {
                MessageHistoryTable.select {
                    MessageHistoryTable.sender eq id
                }
            }

            is Group -> {
                MessageHistoryTable.select {
                    MessageHistoryTable.group eq id
                }
            }

            else -> throw IllegalArgumentException("Unsupported contact type: $contact")
        }.andWhere {
            MessageHistoryTable.ids eq idsString
        }.firstOrNull()?.let {
            it[MessageHistoryTable.message]
        }
    }?.deserializeMiraiCode(contact)
        ?.let {
            return it
        }


    if (contact !is RoamingSupported) {
        return null
    }

    return contact.roamingMessages
        .getMessagesIn(time.toLong() - 10L, time.toLong() + 10L)
        .firstOrNull {
            it.ids.contentEquals(ids)
        }
}

//@jvmblockingbridge
suspend fun MessageChain.roaming(contact: Contact) = sourceOrNull?.roaming(contact)