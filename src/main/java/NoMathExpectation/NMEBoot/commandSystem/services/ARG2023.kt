package NoMathExpectation.NMEBoot.commandSystem.services

import NoMathExpectation.NMEBoot.Main
import NoMathExpectation.NMEBoot.commandSystem.Alias.alias
import NoMathExpectation.NMEBoot.commandSystem.Executable
import NoMathExpectation.NMEBoot.commandSystem.NormalUser
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChainBuilder

object ARG2023 : Executable {
    override fun appendHelp(mcb: MessageChainBuilder, e: MessageEvent) = mcb

    override fun onMessage(e: MessageEvent, user: NormalUser, command: String, miraiCommand: String): Boolean {
        val from = e.subject.alias()
        val args = command.removePrefix("//").split("\\s+".toRegex())
        return when (args[0].lowercase()) {
            "xllylc" -> {
                Main.INSTANCE.launch { from.sendMessage("相和") }
                true
            }

            "arg" -> {
                when (args.getOrNull(1)?.toIntOrNull()) {
                    15 -> {
                        Main.INSTANCE.launch {
                            from.sendMessage("错位的时空，好像快进了一格")
                            from.sendMessage("https://www.aliyundrive.com/s/5teB9PiTyXq\n密码：h44i")
                        }
                    }

                    else -> {
                        Main.INSTANCE.launch { from.sendMessage("回忆中的位置有所偏差，请重试") }
                    }
                }
                true
            }

            else -> false
        }
    }
}