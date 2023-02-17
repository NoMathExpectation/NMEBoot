package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.sending.asCustom
import NoMathExpectation.NMEBoot.utils.MessageHistory
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.usePermission
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import kotlin.random.Random

object CommandHello : SimpleCommand(
    plugin,
    "hello",
    description = "发送 \"Hello, world!\"",
    parentPermission = usePermission
) {
    @Handler
    suspend fun CommandSender.handle() = with(asCustom()) {
        sendMessage("Hello, world!")
    }
}

object CommandRepeat : SimpleCommand(
    plugin,
    "repeat",
    description = "复读机",
    parentPermission = usePermission
) {
    @Handler
    suspend fun CommandSender.handle(message: String) = with(asCustom()) {
        sendMessage(message)
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
    suspend fun CommandSender.handle(count: Long = Random.nextLong(1, 51)) = with(asCustom()) {
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
    suspend fun MemberCommandSender.handle() = with(asCustom()) {
        val history = MessageHistory.randomAsMessage(group.id, origin.bot.id) ?: run {
            sendMessage("找不到历史消息。")
            return@with
        }

        sendMessage(history.first)
        sendMessage(history.second)
    }
}