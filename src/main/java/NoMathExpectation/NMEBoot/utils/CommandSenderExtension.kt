package NoMathExpectation.NMEBoot.utils

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.buildForwardMessage

suspend fun CommandSender.sendFoldMessage(vararg messages: Message) = if (subject != null && bot != null) {
    sendMessage(buildForwardMessage(subject!!) {
        messages.forEach {
            bot!! says it
        }
    })
} else {
    messages.forEach {
        sendMessage(it)
    }
    null
}

suspend fun CommandSender.sendFoldMessage(vararg messages: String) = if (subject != null && bot != null) {
    sendMessage(buildForwardMessage(subject!!) {
        messages.forEach {
            bot!! says it
        }
    })
} else {
    messages.forEach {
        sendMessage(it)
    }
    null
}