package NoMathExpectation.NMEBoot.sending

import NoMathExpectation.NMEBoot.commandSystem.Alias.alias
import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.botEventChannel
import NoMathExpectation.NMEBoot.utils.plugin
import kotlinx.coroutines.delay
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.events.MessagePostSendEvent
import net.mamoe.mirai.event.events.MessagePreSendEvent
import net.mamoe.mirai.event.events.isSuccess
import net.mamoe.mirai.message.data.ForwardMessage
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildForwardMessage
import kotlin.random.Random
import kotlin.random.nextLong

internal object SendingConfig : AutoSavePluginConfig("sending") {
    val enableAlias by value(true)
    val foldLength by value(150)
    val foldLineCount by value(10)
    val minDelay by value(500L)
    val maxDelay by value(1000L)

    val recallTime by value(-1L)

    init {
        reload {
            plugin.reloadPluginConfig(this)
        }
    }
}

fun Message.checkFold(): Boolean {
    if (this is ForwardMessage || (this is MessageChain && get(ForwardMessage) != null)) {
        return false
    }

    val content = contentToString()
    return content.length > SendingConfig.foldLength || content.count { it == '\n' } > SendingConfig.foldLineCount
}

private suspend fun MessagePreSendEvent.inspect() {
    var message = message

    if (message is MessageChain && message[ForwardMessage.Key] != null) {
        message = message[ForwardMessage.Key]!!
    }

    if (SendingConfig.enableAlias) {
        message = message.alias(target)
    }

    if (message.checkFold()) {
        message = buildForwardMessage(bot.asFriend) {
            bot says message
        }
    }

    this.message = message

    val delay = Random.nextLong(SendingConfig.minDelay..SendingConfig.maxDelay)
    delay(delay)
}

private fun MessagePostSendEvent<*>.inspect() {
    if (isSuccess && SendingConfig.recallTime >= 0) {
        receipt?.recallIn(SendingConfig.recallTime)
    }
}

internal fun inspectSendEvents() {
    botEventChannel.subscribeAlways<MessagePreSendEvent>(priority = EventPriority.MONITOR) { inspect() }
    botEventChannel.subscribeAlways<MessagePostSendEvent<*>>(priority = EventPriority.MONITOR) { inspect() }
}