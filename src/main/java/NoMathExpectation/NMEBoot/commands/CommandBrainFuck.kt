package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.usePermission
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig

object CommandBrainFuck : SingleStringCommand(
    plugin,
    "brainfuck",
    "bf",
    usage = "${CommandManager.commandPrefix}bf <expr>",
    description = "Eval brainfuck.",
    parentPermission = usePermission
) {
    private object Config : AutoSavePluginConfig("brainfuck") {
        val opsLimit: Long by value(1000_0000L)
        val outputLimit: Int by value(1000)

        init {
            reload {
                plugin.reloadPluginConfig(this)
            }
        }
    }

    override suspend fun CommandContext.handle(text: String) {
        var output = buildString {
            val cells: MutableMap<Long, Long> = mutableMapOf()
            var p = 0L
            var index = 0
            var ops = 0L
            while (index < text.length) {
                if (++ops > Config.opsLimit) {
                    append("\nTime limit exceeded.")
                    break
                }

                when (text[index]) {
                    '>' -> p++
                    '<' -> p--
                    '+' -> cells[p] = cells.getOrPut(p) { 0 } + 1
                    '-' -> cells[p] = cells.getOrPut(p) { 0 } - 1
                    '.' -> append(cells.getOrDefault(p, 0).toInt().toChar())
                    ',' -> {
                        if (++index < text.length) {
                            cells[p] = text[index].code.toLong()
                        } else {
                            append("\nNo character found after comma.")
                            return@buildString
                        }
                    }

                    '[' -> if (cells.getOrDefault(p, 0) == 0L) {
                        var level = 0
                        while (true) {
                            if (++index >= text.length) {
                                append("\nNo closing bracket found.")
                                return@buildString
                            }
                            when (text[index]) {
                                ']' -> if (--level < 0) {
                                    break
                                }

                                '[' -> level++
                            }
                        }
                    }

                    ']' -> if (cells.getOrDefault(p, 0) != 0L) {
                        var level = 0
                        while (true) {
                            if (--index < 0) {
                                append("\nNo opening bracket found.")
                                return@buildString
                            }
                            when (text[index]) {
                                '[' -> if (--level < 0) {
                                    break
                                }

                                ']' -> level++
                            }
                        }
                    }
                }
                index++
            }
        }

        if (output.length > Config.outputLimit) {
            output = output.substring(0 until Config.outputLimit) + "\n Output limit exceeded."
        }
        if (output.isEmpty()) {
            output = "No output."
        }

        sender.sendMessage(output)
    }
}