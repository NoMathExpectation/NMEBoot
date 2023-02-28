package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.commandSystem.Alias
import NoMathExpectation.NMEBoot.utils.hasAdminPermission
import NoMathExpectation.NMEBoot.utils.isGroupAdmin
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.usePermission
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import java.util.regex.PatternSyntaxException

object CommandAlias : CompositeCommand(
    plugin,
    "alias",
    description = "别称",
    parentPermission = usePermission
) {
    @SubCommand
    @Description("显示此帮助")
    suspend fun MemberCommandSender.help() = sendMessage(Alias.sendHelp())

    @SubCommand
    @Description("显示全部别称")
    suspend fun MemberCommandSender.show() = sendMessage(Alias.toString(group.id).ifBlank { "没有正在使用的别称" })

    @SubCommand
    @Description("添加新别称，使用\\\"来逃避")
    suspend fun MemberCommandSender.add(fromRegex: String, to: String, pos: Int? = null) {
        try {
            fromRegex.toRegex()
        } catch (e: PatternSyntaxException) {
            sendMessage("错误的regex格式")
            return
        }

        if (pos == null) {
            Alias.add(group.id, Triple(fromRegex, to, false))
        } else {
            try {
                Alias.add(group.id, Triple(fromRegex, to, false), pos)
            } catch (e: IndexOutOfBoundsException) {
                sendMessage("错误的位置")
                return
            }
        }

        sendMessage("已保存: $fromRegex -> $to")
    }

    @SubCommand
    @Description("移除一条别称")
    suspend fun MemberCommandSender.remove(pos: Int) {
        try {
            val p = Alias.remove(group.id, pos, isGroupAdmin())
            sendMessage("已移除: ${p.first} -> ${p.second}")
        } catch (e: IndexOutOfBoundsException) {
            sendMessage("未找到对应别称")
        } catch (e: IllegalStateException) {
            sendMessage("你没有权限移除此别称")
        }
    }

    @SubCommand
    @Description("将一条别称移动到新的位置")
    suspend fun MemberCommandSender.move(from: Int, to: Int) {
        try {
            Alias.move(group.id, from, to)
            sendMessage("操作成功")
        } catch (e: IndexOutOfBoundsException) {
            sendMessage("未找到对应别称")
        }
    }

    @SubCommand
    @Description("保护/取消保护别称")
    suspend fun MemberCommandSender.protect(pos: Int) {
        try {
            if (Alias.protect(group.id, pos)) {
                sendMessage("已保护此别称")
            } else {
                sendMessage("已取消保护此别称")
            }
        } catch (e: IndexOutOfBoundsException) {
            sendMessage("未找到对应别称")
        }
    }

    @SubCommand
    @Description("清除所有别称")
    suspend fun MemberCommandSender.clear() {
        if (!isGroupAdmin()) {
            sendMessage("你没有权限执行此指令")
            return
        }

        Alias.clear(group.id)
        sendMessage("已清除所有别称")
    }

    @SubCommand
    @Description("导入别称")
    suspend fun MemberCommandSender.import(text: String) {
        if (!hasAdminPermission()) {
            sendMessage("你没有权限执行此指令")
            return
        }

        Alias.clear(group.id)

        text.deserializeMiraiCode(group)
            .contentToString()
            .split("\n?\\d+\\. ".toRegex())
            .filter(String::isNotBlank)
            .associate {
                it.split(" -> ")
                    .let { it[0] to it[1] }
            }.forEach { (first, second) -> Alias.add(group.id, Triple(first, second, false)) }
    }
}