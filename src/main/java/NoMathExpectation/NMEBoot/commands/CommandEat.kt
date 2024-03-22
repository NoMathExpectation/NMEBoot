package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.MessageHistoryTable
import NoMathExpectation.NMEBoot.utils.SQLRandom
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.usePermission
import net.mamoe.mirai.console.command.*
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.PluginDataExtensions.withEmptyDefault
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildMessageChain
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object CommandEat : SimpleCommand(
    plugin,
    "eat",
    description = "吃什么",
    parentPermission = usePermission
) {
    @Handler
    suspend fun CommandSender.handle(pronoun: String = "我") {
        val person = when (pronoun.trim().lowercase()) {
            "我", "俺" -> "你"
            "我们", "俺们" -> "你们"
            "吾" -> "您"
            "me", "i", "we" -> "you"
            "my", "our" -> "your"
            "你", "您", "你们", "您们", "机器人", "高性能机器人", "bot", "robot", "nmeboot", "atri", "亚托莉", "萝卜子", "萝卜" -> {
                sendMessage("高性能机器人不需要吃饭捏\uD83D\uDE0E")
                return
            }

            else -> pronoun
        }

        if (subject !is Group || EatConfig.isEmpty(subject!!.id)) transaction {
            var foodMessageSelect =
                MessageHistoryTable.selectAll()
                    .where { (MessageHistoryTable.message like "%吃%") or (MessageHistoryTable.message like "%炫%") }
            bot?.let { foodMessageSelect = foodMessageSelect.andWhere { MessageHistoryTable.sender neq it.id } }

            val foodMessageString = foodMessageSelect.orderBy(SQLRandom())
                .limit(1)
                .first()[MessageHistoryTable.message]

            "我建议$person${"[吃炫](.*)".toRegex().find(foodMessageString)!!.value}".deserializeMiraiCode(subject)
        }.let { sendMessage(it) } else {
            sendMessage(buildMessageChain {
                +"我建议"
                +person
                +"吃"
                +EatConfig.random(subject!!.id).deserializeMiraiCode(subject)
            })
        }
    }
}

internal object CommandEatComposite : CompositeCommand(
    plugin,
    "seteat",
    description = "配置吃什么",
    parentPermission = usePermission
) {
    @SubCommand
    @Description("显示帮助")
    suspend fun CommandSender.help() = sendMessage(buildMessageChain {
        +"${CommandManager.commandPrefix}eat...\n"
        +"<无后缀>: 帮助你决定吃什么\n"
        +"${CommandManager.commandPrefix}seteat...\n"
        +"add <dish>: 添加菜品\n"
        +"remove <dish>: 删除菜品\n"
        +"removeIndex <index>: 删除菜品\n"
        +"show: 显示菜单\n"
    })

    @SubCommand
    @Description("添加菜品")
    suspend fun MemberCommandSender.add(dish: MessageChain) {
        EatConfig.add(group.id, dish.serializeToMiraiCode())
        sendMessage(buildMessageChain {
            +"已加入菜单："
            +dish
        })
    }

    @SubCommand("removeIndex")
    @Description("删除菜品")
    suspend fun MemberCommandSender.remove(index: Int) {
        try {
            sendMessage("已删除菜品：${EatConfig.remove(group.id, index - 1)}")
        } catch (e: IndexOutOfBoundsException) {
            sendMessage("未找到对应菜品")
        }
    }

    @SubCommand
    @Description("删除菜品")
    suspend fun MemberCommandSender.remove(dish: String) {
        EatConfig.remove(group.id, dish)?.let {
            sendMessage("已删除菜品：$it")
        } ?: sendMessage("未找到对应菜品")
    }

    @SubCommand
    @Description("显示菜单")
    suspend fun MemberCommandSender.show() {
        sendMessage("当前菜单：\n${EatConfig.show(group.id)}")
    }
}

object EatConfig : AutoSavePluginConfig("eat") {
    private val menus by value<MutableMap<Long, MutableList<String>>>().withEmptyDefault()

    init {
        reload {
            plugin.reloadPluginConfig(this)
        }
    }

    fun add(id: Long, dish: String) = menus[id].add(dish)

    fun remove(id: Long, index: Int) = menus[id].removeAt(index)

    fun remove(id: Long, dish: String) = menus[id].find { dish in it }?.also { menus[id].remove(it) }

    fun show(id: Long) = menus[id].withIndex().joinToString("\n") { "${it.index + 1}. ${it.value}" }

    fun isEmpty(id: Long) = menus[id].isEmpty()

    fun random(id: Long) = menus[id].random()
}