package NoMathExpectation.NMEBoot.utils

import NoMathExpectation.NMEBoot.Main
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupMessageSyncEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.MessageChain

internal object Repeat : AutoSavePluginConfig("repeat") {
    var enabledGroup: List<Long> by value()
    var repeatCount: Int by value(5)

    val record = mutableMapOf<Long, Pair<MessageChain, Int>>()

    private fun repeat(id: Long, msg: MessageChain): Boolean {
        if (id !in enabledGroup) {
            return false
        }

        var pair = record[id]
        if (pair == null || !pair.first.contentEquals(msg)) {
            pair = msg to 1
            record[id] = pair
        } else {
            pair = msg to pair.second + 1
            record[id] = pair
        }

        return pair.second == repeatCount
    }

    suspend fun repeat(event: MessageEvent) = when (event) {
        is GroupMessageEvent -> {
            if (repeat(event.group.id, event.message)) {
                event.group.sendMessage(event.message)
                true
            } else {
                false
            }
        }

        is GroupMessageSyncEvent -> {
            if (repeat(event.group.id, event.message)) {
                event.group.sendMessage(event.message)
                true
            } else {
                false
            }
        }

        else -> false
    }

    fun startMonitor() = Main.INSTANCE.globalEventChannel().subscribeAlways<MessageEvent> { e ->
        if (repeat(e)) {
            Main.INSTANCE.logger.debug("Message repeated: ${e.message.contentToString()}")
        }
    }

    init {
        Main.INSTANCE.reloadPluginConfig(this)
        check(repeatCount > 1) { "repeatCount must be greater than 1, but found $repeatCount." }
    }
}