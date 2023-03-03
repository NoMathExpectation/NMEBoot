package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.commandSystem.Alias
import NoMathExpectation.NMEBoot.commandSystem.AliasItem
import NoMathExpectation.NMEBoot.sending.AliasIgnore
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
    suspend fun MemberCommandSender.show() {
        val text = Alias.toString(group.id)
        if (text.isBlank()) {
            sendMessage("没有正在使用的别称")
        } else {
            sendMessage(AliasIgnore + text)
        }
    }

    @SubCommand
    @Description("添加新别称")
    suspend fun MemberCommandSender.add(
        fromRegex: String,
        to: String,
        applyIn: Boolean = true,
        applyOut: Boolean = true,
        protected: Boolean = false,
        pos: Int? = null
    ) {
        try {
            fromRegex.toRegex()
        } catch (e: PatternSyntaxException) {
            sendMessage("错误的regex格式")
            return
        }

        val p = AliasItem(fromRegex, to, applyIn, applyOut, protected)
        if (pos == null) {
            Alias.add(group.id, p)
        } else {
            try {
                Alias.add(group.id, p, pos)
            } catch (e: IndexOutOfBoundsException) {
                sendMessage("错误的位置")
                return
            }
        }

        sendMessage(AliasIgnore + "已保存: \"$fromRegex\" -> \"$to\"")
    }

    @SubCommand
    @Description("移除一条别称")
    suspend fun MemberCommandSender.remove(pos: Int) {
        try {
            val p = Alias.remove(group.id, pos, isGroupAdmin())
            sendMessage(AliasIgnore + "已移除: \"${p.from}\" -> \"${p.to}\"")
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
        if (!isGroupAdmin()) {
            sendMessage("你没有权限执行此指令")
            return
        }

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
    @Description("设置别称属性")
    suspend fun MemberCommandSender.set(
        pos: Int,
        applyIn: Boolean? = null,
        applyOut: Boolean? = null,
        protected: Boolean? = null
    ) {
        if (Alias.isProtected(group.id, pos) && !isGroupAdmin()) {
            sendMessage("被保护的别称")
            return
        }

        try {
            Alias.set(group.id, pos) {
                if (applyIn != null) {
                    this.applyIn = applyIn
                }
                if (applyOut != null) {
                    this.applyOut = applyOut
                }
                if (protected != null) {
                    this.protected = protected
                }
            }
            sendMessage("设置成功")
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

    //@SubCommand //only use when debug
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
            }.forEach { (first, second) -> Alias.add(group.id, AliasItem(first, second)) }
    }
}