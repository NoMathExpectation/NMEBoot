package NoMathExpectation.NMEBoot.simBasedCommand.commands

import NoMathExpectation.NMEBoot.simBasedCommand.context.CommandContext
import NoMathExpectation.NMEBoot.utils.*
import com.mojang.brigadier.CommandDispatcher

fun @BrigadierDsl CommandDispatcher<CommandContext<*>>.commandPermission() = register("permission") {
    filter {
        hasPermission("command.permission")
    }

    literalArgument("set") {
        literalArgument("@s") {
            stringArgument("permId") {
                boolArgument("value") {
                    handle {
                        val perm = get<String>("permId")
                        val value = get<Boolean>("value")
                        source.setPermission(perm, value)

                        source.reply("已${if (value) "给予" else "剥夺"} ${source.id} 的权限 $perm")
                    }
                }

                handle {
                    val perm = get<String>("permId")
                    source.setPermission(perm, null)
                    source.reply("已清空 ${source.id} 的权限 $perm")
                }
            }
        }
    }
}