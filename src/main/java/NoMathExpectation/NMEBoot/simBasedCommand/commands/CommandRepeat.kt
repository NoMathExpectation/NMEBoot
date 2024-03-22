package NoMathExpectation.NMEBoot.simBasedCommand.commands

import NoMathExpectation.NMEBoot.simBasedCommand.context.CommandContext
import NoMathExpectation.NMEBoot.utils.*
import com.mojang.brigadier.CommandDispatcher

fun @BrigadierDsl CommandDispatcher<CommandContext<*>>.commandRepeat() = register("repeat") {
    stringArgument("text", StringArgumentCaptureType.GREEDY) {
        handle {
            val text = get<String>("text")
            source.reply(text)
        }
    }
}