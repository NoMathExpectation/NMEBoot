package NoMathExpectation.NMEBoot.commandSystem

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.wolframAlpha.Conversation
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginData
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.buildForwardMessage
import net.mamoe.mirai.message.data.buildMessageChain
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object NormalUserStats : AutoSavePluginData("normalUser") {
    var checkInCount: Int by value()
    var checkInCountTotal: Int by value()
    var checkInDate: String by value(LocalDate.MIN.format(DateTimeFormatter.ISO_LOCAL_DATE))
    var messageCountDate: String by value(LocalDate.MIN.format(DateTimeFormatter.ISO_LOCAL_DATE))
    var user: Set<ByteArray> by value()

    init {
        reload {
            plugin.reloadPluginData(this)
        }
    }

    fun getStatList() = buildMessageChain {
        +".//stat|stats ...\n"
        +"general : 通用\n"
        +"checkin : 签到时间排行榜\n"
        +"streak : 连续签到排行榜\n"
        +"chat|message : 聊天排行榜\n"
        +"chatDaily|messageDaily : 日聊天排行榜\n"
    }

    fun getStat(name: String, group: Long) = when (name.lowercase()) {
        "general" -> getGeneral(group)
        "checkin" -> getCheckinTime(group)
        "streak" -> getCheckinStreak(group)
        "chat", "message" -> getMessageCount(group)
        "chatdaily", "messagedaily" -> getMessageCountDaily(group)
        else -> buildMessageChain { + "未知的项目" }
    }

    fun getGeneral(group: Long) = buildMessageChain {
        + "总签到数："
        + checkInCountTotal.toString()
        + "\n提问数："
        + Conversation.queryTimes.toString()
        + "\n今日消息数："
        + NormalUser.getUsers().values.sumOf { it.totalMessageCountDaily }.toString()
        + "\n群今日消息数："
        + NormalUser.getUsers().values.sumOf { it.getMessageCountDaily(group) }.toString()
        + "\n总消息数："
        + NormalUser.getUsers().values.sumOf { it.totalMessageCount }.toString()
        + "\n群总消息数："
        + NormalUser.getUsers().values.sumOf { it.getMessageCount(group) }.toString()
    }

    fun getCheckinTime(group: Long) = buildMessageChain {
        + "签到时间排名：\n"

        val groupUsers = mutableSetOf<Long>()
        Bot.instances.forEach { it.getGroup(group)?.members?.forEach { m -> groupUsers.add(m.id) } }
        val sortedUser = NormalUser.getUsers().values.filter { groupUsers.contains(it.id) && it.isCheckedIn }.sortedBy { it.checkInRank }

        for (i in sortedUser.indices) {
            val checkinTime = sortedUser[i].lastCheckInTime

            + (i + 1).toString()
            + ". "
            + sortedUser[i].name
            + ":  "

            + checkinTime.hour.toString()
            + ":"
            + checkinTime.minute.toString()
            + ":"
            + checkinTime.second.toString()
            + "."
            + when (val checkinTimeMillis = checkinTime.nano / 1000000) {
                in 0..9 -> "00$checkinTimeMillis"
                in 10..99 -> "0$checkinTimeMillis"
                else -> checkinTimeMillis.toString()
            }

            + "\n"
        }
    }

    fun getCheckinStreak(group: Long) = buildMessageChain {
        + "连续签到排名：\n"

        val groupUsers = mutableSetOf<Long>()
        Bot.instances.forEach { it.getGroup(group)?.members?.forEach { m -> groupUsers.add(m.id) } }
        val sortedUser = NormalUser.getUsers().values.filter { groupUsers.contains(it.id) && it.isCheckedIn }.sortedBy { it.checkInStreak }.reversed()

        for (i in sortedUser.indices) {
            + (i + 1).toString()
            + ". "
            + sortedUser[i].name
            + ":  "

            + sortedUser[i].checkInStreak.toString()

            + "天\n"
        }
    }

    fun getMessageCount(group: Long) = buildMessageChain {
        + "群消息数排名：\n"

        val sortedUser = NormalUser.getUsers().values.filter { it.getMessageCount(group) > 0 }.sortedBy { it.getMessageCount(group) }.reversed()
        for (i in sortedUser.indices) {
            + (i + 1).toString()
            + ". "
            + sortedUser[i].name
            + ": "
            + sortedUser[i].getMessageCount(group).toString()
            + "条消息\n"
        }
    }

    fun getMessageCountDaily(group: Long) = buildMessageChain {
        + "群日消息数排名：\n"

        val sortedUser = NormalUser.getUsers().values.filter { it.getMessageCountDaily(group) > 0 }.sortedBy { it.getMessageCountDaily(group) }.reversed()
        for (i in sortedUser.indices) {
            + (i + 1).toString()
            + ". "
            + sortedUser[i].name
            + ": "
            + sortedUser[i].getMessageCountDaily(group).toString()
            + "条消息\n"
        }
    }

    fun getMessageCountAndDailyAsForwardMessage(contact: Contact) = buildForwardMessage(contact) {
        val id = contact.id
        val top1Day = NormalUser.getUsers().values.filter { it.getMessageCountDaily(id) > 0 }.sortedBy { it.getMessageCountDaily(id) }.reversed()[0]
        val top1 = NormalUser.getUsers().values.filter { it.getMessageCount(id) > 0 }.sortedBy { it.getMessageCount(id) }.reversed()[0]
        top1Day.id named top1Day.name says getMessageCountDaily(id)
        top1.id named top1.name says getMessageCount(id)
    }
}