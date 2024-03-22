package NoMathExpectation.NMEBoot.simBasedCommand.commands

import NoMathExpectation.NMEBoot.simBasedCommand.context.CommandContext
import NoMathExpectation.NMEBoot.simBasedCommand.context.permissionId
import NoMathExpectation.NMEBoot.utils.*
import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.flow.firstOrNull
import love.forte.simbot.component.kook.KookGuild

fun @BrigadierDsl CommandDispatcher<CommandContext<*>>.commandWhoAmI() = register("whoami") {
    stringArgument("name") {
        filter {
            hasPermission("command.whoami")
        }

        handle {
            val globalSubject = source.globalSubject
            if (globalSubject is KookGuild) {
                val (id, name) = globalSubject
                    .members
                    .asFlow()
                    .firstOrNull { get<String>("name") in it.name }
                    ?.let { it.permissionId to it.name }
                    ?: kotlin.run {
                        source.send("未找到此用户")
                        return@handle
                    }
                source.send("$name 是 $id")
            } else {
                source.send("不支持查看用户")
            }
        }
    }

    handle {
        source.send("你是 ${source.permissionIds}")
    }
}