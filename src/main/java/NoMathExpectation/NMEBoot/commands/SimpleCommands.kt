package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.Main
import NoMathExpectation.NMEBoot.inventory.modules.Luck
import NoMathExpectation.NMEBoot.inventory.modules.Reloading
import NoMathExpectation.NMEBoot.sending.Cooldown
import NoMathExpectation.NMEBoot.utils.*
import NoMathExpectation.NMEBoot.wolframAlpha.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration

internal fun registerCommands() {
    //composite commands
    CommandCard.register()
    CommandChart.register()
    CommandAlias.register()
    CommandEatComposite.register()
    //CommandExport.register()

    //simple commands
    CommandHello.register()
    CommandRepeat.register()
    Command114514.register()
    CommandHistory.register()
    CommandLuck.register()
    CommandReload.register()
    CommandStats.register()
    CommandWordle.register()
    CommandFeedback.register()
    CommandEat.register()
    CommandAsk.register()
    CommandEval.register()
    CommandBrainFuck.register()
    CommandBefunge.register()
    CommandCooldown.register()
}

object CommandHello : SimpleCommand(
    plugin,
    "hello",
    description = "发送 \"Hello, world!\"",
    parentPermission = usePermission
) {
    @Handler
    suspend fun CommandSender.handle() {
        sendMessage("Hello, world!")
    }
}

object CommandRepeat : RawCommand(
    plugin,
    "repeat",
    "repeat!",
    usage = "${CommandManager.commandPrefix}repeat <message>",
    description = "复读机",
    parentPermission = usePermission
) {
    private val regex = "${CommandManager.commandPrefix}$primaryName!?[\\s\\h\\v]*".toRegex()

    override suspend fun CommandContext.onCommand(args: MessageChain) {
        sender.sendMessage(
            originalMessage.quote()
                    + originalMessage.serializeToMiraiCode()
                .replaceFirst(regex, "")
                .deserializeMiraiCode(sender.subject)
        )
    }
}

object Command114514 : SimpleCommand(
    plugin,
    "114514",
    "1919810",
    description = "114514",
    parentPermission = usePermission
) {
    @Handler
    suspend fun CommandSender.handle(count: Long = Random.nextLong(1, 51)) {
        when {
            count < 0L -> sendMessage("earthOL.physics.ThermalException:沼气自发地回到了化粪池")
            count == 0L -> sendMessage("什么也没有发生")
            count > 1000L -> sendMessage("程序被化粪池过浓的沼气熏死了")
            else -> sendMessage("哼，哼，哼，${"啊".repeat(count.toInt())}！")
        }
    }
}

object CommandHistory : SimpleCommand(
    plugin,
    "history",
    description = "随机一条历史消息",
    parentPermission = usePermission
) {
    @Handler
    suspend fun MemberCommandSender.handle() {
        val history = MessageHistory.randomAsMessage(group.id, bot.id) ?: run {
            sendMessage("找不到历史消息。")
            return
        }

        sendMessage(history.first)
        sendMessage(history.second)
    }
}

object CommandLuck : SimpleCommand(
    plugin,
    "luck",
    description = "测测你今天的运气",
    parentPermission = usePermission
) {
    @Handler
    suspend fun handle(context: CommandContext) {
        val sender = context.sender.safeCast<AbstractUserCommandSender>() ?: run {
            context.sender.sendMessage("只有用户才能使用这个指令")
            return
        }
        val luck = Luck[sender.user.id].luck

        sender.sendMessage(buildMessageChain {
            +context.originalMessage.quote()
            +"你今天的运气是: $luck"
            if (luck >= 100) {
                +"！\n"
                +"这运气也太好了吧！"
            }
            if (luck <= 0) {
                +"！\n"
                +"这运气也太差了吧......"
            }
        })
    }
}

object CommandReload : SimpleCommand(
    plugin,
    "reload",
    description = "重载数据",
    parentPermission = adminPermission
) {
    @Handler
    suspend fun CommandSender.handle(name: String) {
        if (Reloading.reload(name)) {
            sendMessage("重载成功")
        } else {
            sendMessage("重载失败")
        }
    }
}

object CommandWordle : RawCommand(
    plugin,
    "wordle",
    "w",
    usage = "${CommandManager.commandPrefix}wordle help",
    description = "wordle",
    parentPermission = usePermission
) {
    val wordle by lazy { Main.wordle }

    override suspend fun CommandSender.onCommand(args: MessageChain) {
        if (this !is AbstractUserCommandSender) {
            sendMessage("只有用户才能使用这个指令")
            return
        }

        val argsText = args.map(Message::contentToString)
            .filter(String::isNotBlank)
            .toTypedArray()
        if (argsText.isEmpty()) {
            wordle.sendHelp(subject)
            return
        }

        try {
            when (argsText[0]) {
                "help" -> wordle.sendHelp(subject)
                "new" -> sendMessage(wordle.parseAndNewWordle(subject.id, argsText))
                "show" -> sendMessage(wordle.getWordleMessage(subject.id, false))
                else -> sendMessage(wordle.validateAnswer(subject.id, argsText[0]))
            }
        } catch (e: Exception) {
            sendMessage(e.message ?: "发生了一个错误")
        }
    }
}

object CommandFeedback : RawCommand(
    plugin,
    "feedback",
    usage = "${CommandManager.commandPrefix}feedback <text>",
    description = "给作者反馈",
    parentPermission = usePermission
) {
    private val regex = "${CommandManager.commandPrefix}$primaryName[\\s\\h\\v]*".toRegex()

    override suspend fun CommandContext.onCommand(args: MessageChain) {
        if (sender !is AbstractUserCommandSender) {
            sender.sendMessage("只有用户才能使用这个指令")
            return
        }

        logger.warning(buildString {
            append("来自用户 ${sender.name} (id: ${sender.user?.id}) ")
            sender.subject.safeCast<Group>()?.let {
                append("，群 ${it.name} (id: ${it.id}) ")
            }
            append("的反馈：")
            append(originalMessage.serializeToMiraiCode().replaceFirst(regex, ""))
        })
        sender.sendMessage("反馈已发送。")
    }
}

object CommandAsk : SingleStringCommand(
    plugin,
    "ask",
    usage = "${CommandManager.commandPrefix}ask <text>",
    description = "Ask wolfram.",
    parentPermission = usePermission
) {
    override suspend fun CommandContext.handle(text: String) {
        var message = kotlin.runCatching { Conversation[sender.subject?.id ?: -1].query(text) }
            .getOrElse {
                it.printStackTrace()
                "An error occurred."
            }
            .toPlainText()
            .toMessageChain()
        kotlin.runCatching { originalMessage.quote() }
            .getOrNull()
            ?.let {
                message = it + message
            }
        sender.sendMessage(message)
    }
}

object CommandEval : SingleStringCommand(
    plugin,
    "eval",
    usage = "${CommandManager.commandPrefix}eval <expr>",
    description = "Eval kotlin.",
    parentPermission = usePermission
) {
    override suspend fun CommandContext.handle(text: String) {
        val name = "mirai-eval-${UUID.randomUUID()}"
        val timeout = 30L
        val memory = (256L * 1024 * 1024).toString()

        val quoted = originalMessage[QuoteReply.Key]?.source

        val script = """
            data class Message(val from: Long, val to: Long, val text: String)
            
            val quoted: Message? = ${
            quoted?.let {
                """
                        Message(
                            ${quoted.fromId},
                            ${quoted.targetId},
                            "${
                    quoted.originalMessage.serializeToMiraiCode()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("$", "${'$'}{'$'}")
                }",
                        )
                    """.trimIndent()
            } ?: "null"
        }
            
            try {
                val result: Any? = run {
                    $text
                }
                
                if (result !is Unit) {
                    println()
                    print(result)
                }
            } catch (e: Throwable) {
                println()
                print(e.stackTraceToString())
            }
            
        """.trimIndent()

        var tle = false
        val process = withContext(Dispatchers.IO) {
            val process = Runtime.getRuntime()
                .exec(arrayOf("docker", "run", "--rm", "-m", memory, "--name", name, "-i", "kscripting/kscript", "-"))
            process.outputStream.use {
                it.write(script.toByteArray())
            }
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                tle = true
                Runtime.getRuntime().exec("docker stop $name")
            }
            process
        }

        val err = process.errorStream.bufferedReader().use { it.readText() }
        val out = process.inputStream.bufferedReader().use { it.readText() }

        val output = arrayOf(out, err, if (tle) "Time limit exceeded." else null)
            .filterNotNull()
            .filter(String::isNotBlank)
            .joinToString("\n")
            .takeIf(String::isNotEmpty) ?: "No output."

        var message = output.deserializeMiraiCode(sender.subject)
        kotlin.runCatching { originalMessage.quote() }
            .getOrNull()
            ?.let {
                message = it + message
            }
        sender.sendMessage(message)
    }
}

object CommandCooldown : SimpleCommand(
    plugin,
    "cooldown",
    "cd",
    description = "设置冷却",
    parentPermission = usePermission
) {
    @Handler
    suspend fun MemberCommandSender.handle(time: String) {
        if (!hasAdminPermission() && !isGroupAdmin()) {
            return
        }

        val duration = Duration.parse(time)
        if (duration.isNegative()) {
            sendMessage("请提供一个非负的间隔")
            return
        }

        Cooldown[group.id] = FixedDelayUseCounter(1, duration)
        sendMessage("已设置指令间隔为$time")
    }

}