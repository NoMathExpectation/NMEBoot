package NoMathExpectation.NMEBoot.simBasedCommand.commands

import NoMathExpectation.NMEBoot.simBasedCommand.context.CommandContext
import NoMathExpectation.NMEBoot.utils.*
import com.mojang.brigadier.CommandDispatcher

fun @BrigadierDsl CommandDispatcher<CommandContext<*>>.commandHello() = register("hello") {
    stringArgument("pronoun", StringArgumentCaptureType.WORD) {
        handle {
            source.send("Hello, ${get<String>("pronoun")}!")
        }
    }

    handle {
        source.send("Hello, world!")
    }
}