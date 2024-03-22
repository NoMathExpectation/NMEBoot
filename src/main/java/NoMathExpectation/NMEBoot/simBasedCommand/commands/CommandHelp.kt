package NoMathExpectation.NMEBoot.simBasedCommand.commands

import NoMathExpectation.NMEBoot.simBasedCommand.context.CommandContext
import NoMathExpectation.NMEBoot.utils.BrigadierDsl
import NoMathExpectation.NMEBoot.utils.handle
import NoMathExpectation.NMEBoot.utils.register
import com.mojang.brigadier.CommandDispatcher

fun @BrigadierDsl CommandDispatcher<CommandContext<*>>.commandHelp() = register("help") {
    handle {
        source.reply(this@commandHelp.getSmartUsage(rootNode, source).values.joinToString("\n"))
    }
}