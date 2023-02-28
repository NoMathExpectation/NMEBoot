package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.inventory.modules.Luck
import NoMathExpectation.NMEBoot.inventory.modules.Reloading
import NoMathExpectation.NMEBoot.utils.MessageHistory
import NoMathExpectation.NMEBoot.utils.adminPermission
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.usePermission
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import kotlin.random.Random

internal fun registerCommands() {
    //composite commands
    CommandCard.register()
    CommandChart.register()
    CommandAlias.register()

    //simple commands
    CommandHello.register()
    CommandRepeat.register()
    Command114514.register()
    CommandHistory.register()
    CommandLuck.register()
    CommandReload.register()
    CommandStats.register()
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
    usage = "//repeat <message>",
    description = "复读机",
    parentPermission = usePermission
) {
    private val regex = "${CommandManager.commandPrefix}$primaryName[\\s\\h\\v]*".toRegex()

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

        sender.sendMessage(context.originalMessage.quote() + "你今天的运气是: $luck")
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
