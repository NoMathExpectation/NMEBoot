package NoMathExpectation.NMEBoot.simbot.command

import NoMathExpectation.NMEBoot.simBasedCommand.commands.*
import NoMathExpectation.NMEBoot.simBasedCommand.context.CommandContext
import NoMathExpectation.NMEBoot.simBasedCommand.context.KookMessageEventBasedCommandContext.Companion.Contact
import NoMathExpectation.NMEBoot.simBasedCommand.context.KookMessageEventBasedCommandContext.Companion.MessageChannel
import NoMathExpectation.NMEBoot.simbot.SimConfig
import NoMathExpectation.NMEBoot.utils.CommandDispatcher
import NoMathExpectation.NMEBoot.utils.logger
import com.mojang.brigadier.exceptions.CommandSyntaxException
import love.forte.simbot.application.listeners
import love.forte.simbot.event.MessageEvent
import love.forte.simbot.event.process
import love.forte.simbot.message.buildMessages

object SimbotCommandManager {
    private val dispatcher = CommandDispatcher {
        commandHello()
        commandWhoAmI()
        commandPermission()
        commandRepeat()
        commandHelp()
    }

    internal fun register() {
        CommandContext.register(::MessageChannel)
        CommandContext.register(::Contact)

        SimConfig.app?.listeners {
            process<MessageEvent> {
                var text = it.messageContent.plainText ?: ""
                if (!text.startsWith("//")) {
                    return@process
                }
                text = text.substring(2)

                val info = CommandContext[it]
                val parsed = dispatcher.parse(text, info)

                if (parsed.reader.canRead() || parsed.exceptions.isNotEmpty()) {
                    val exception = parsed.exceptions.values.firstOrNull()
                    logger.info(exception)
                    it.reply(buildMessages {
                        if (exception != null) {
                            +"在第${exception.cursor}个字符处产生了一个错误:\n"
                            +(exception.message ?: "武士走进了妮可的咖啡店，要了一杯甜甜圈。")
                        } else if (parsed.reader.canRead()) {
                            +"在第${parsed.reader.cursor}个字符处发现了多余的字符\n"
                        } else {
                            +"武士走进了妮可的咖啡店，要了一杯甜甜圈。"
                        }
                    })
                    return@process
                }

                kotlin.runCatching {
                    dispatcher.execute(parsed)
                }.onFailure { e ->
                    logger.error(e)
                    it.reply(if (e is CommandSyntaxException) e.localizedMessage else "武士走进了妮可的咖啡店，要了一杯甜甜圈。")
                }
            }
        }
    }
}