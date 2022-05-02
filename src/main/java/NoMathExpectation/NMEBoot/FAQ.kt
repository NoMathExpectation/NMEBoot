package NoMathExpectation.NMEBoot

import NoMathExpectation.NMEBoot.commandSystem.UsingGroup
import kotlinx.coroutines.runBlocking
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupMessageSyncEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import org.jetbrains.annotations.NotNull

object FAQ : AutoSavePluginConfig("faq") {
    private val groups: MutableMap<Long, MutableMap<String, Pair<String, Array<MessageChain>>>> by value()

    val preservedName: Array<String> = arrayOf("new", "save", "discard", "help", "remove")

    private val recordingUser: MutableMap<Long, Long> = HashMap()
    private val recordingContent: MutableMap<Long, MutableList<MessageChain>> = HashMap()

    init {
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> { e -> record(e.subject.id, e.sender.id, e.message) }
        GlobalEventChannel.subscribeAlways<GroupMessageSyncEvent> { e -> record(e.subject.id, e.bot.id, e.message) }
    }

    private fun record(group: Long, id: Long, message: MessageChain) {
        if (!UsingGroup.group.contains(group) || recordingUser[group] != id || message.contentToString().startsWith("//")) {
            return
        }
        recordingContent[group]?.add(message)
    }

    fun start(group: Long, id: Long) {
        if (isRecording(group)) {
            throw IllegalStateException("有成员正在录制")
        }
        recordingUser[group] = id
        recordingContent[group] = ArrayList()
    }

    fun isRecording(group: Long) = recordingUser[group] != null

    fun discard(group: Long) {
        if (!isRecording(group)) {
            throw IllegalStateException("没有正在录制的faq")
        }
        recordingUser.remove(group)
        recordingContent.remove(group)
    }

    fun save(group: Long, name: String, description: String) {
        if (!isRecording(group)) {
            throw IllegalStateException("没有正在录制的faq")
        }

        if (preservedName.contains(name)) {
            throw IllegalArgumentException("不能以保留关键字命名")
        }
        val map = groups.computeIfAbsent(group) { HashMap() }
        if (map.containsKey(name)) {
            throw IllegalArgumentException("已经存在一个相同名字的faq")
        }

        map[name] = Pair(description, recordingContent[group]!!.toTypedArray())
        discard(group)
    }

    @JvmBlockingBridge
    suspend fun send(group: Group, name: String) {
        groups[group.id]?.get(name)?.second?.let {
            for (msg in it) {
                group.sendMessage(msg)
            }
            return
        }
        group.sendMessage("未找到此faq")
    }

    fun remove(group: Long, name: String) = groups[group]?.remove(name) ?: throw NoSuchElementException("未找到此faq")

    @NotNull
    @JvmBlockingBridge
    suspend fun getHelp(group: Contact, isAdmin: Boolean): MessageReceipt<Contact> {
        val mcb = MessageChainBuilder().append(
            "//faq...\n",
            "help :获取此帮助\n"
        )

        if (isAdmin) {
            mcb.append(
                "new :开始录制新的faq\n",
                "save <名字> <描述>:保存正在录制的faq到对应名字\n",
                "discard :丢弃正在录制的faq\n",
                "remove <名字>:移除已录制的faq"
            )
        }

        groups[group.id]?.let {
            for(name in it.keys) {
                it[name]?.let { it1 ->
                    mcb.append("$name :${it1.first}\n")
                }
            }
        }

        return group.sendMessage(mcb.build())
    }
}