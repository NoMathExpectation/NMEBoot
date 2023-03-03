package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.usePermission
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.buildMessageChain

typealias MessageChainBuildFunction = suspend MessageChainBuilder.(Contact?) -> Unit

data class Stats(val page: String, val description: String = "", var content: MessageChainBuildFunction = {})

object CommandStats : SimpleCommand(
    plugin,
    "stats",
    "stat",
    description = "查看统计数据",
    parentPermission = usePermission,
) {
    const val defaultPage = "general"

    private val statPages: MutableMap<String, Stats> = mutableMapOf()

    init {
        statPages["list"] = Stats("list", "可用的统计数据") {
            appendLine("${CommandManager.commandPrefix}$primaryName ...")
            statPages.forEach { (page, stats) ->
                appendLine("$page: ${stats.description}")
            }
        }
    }

    fun appendStats(page: String = defaultPage, description: String = "", content: MessageChainBuildFunction) {
        val stats = statPages.getOrPut(page) { Stats(page, description) }
        stats.content = {
            stats.content(this, it)
            content(this, it)
            appendLine()
        }
    }

    suspend fun getStats(contact: Contact?, page: String = defaultPage) = buildMessageChain {
        val stats = statPages[page] ?: run {
            +"未找到数据。"
            return@buildMessageChain
        }

        stats.content(this, contact)
    }

    @Handler
    suspend fun CommandSender.handle(page: String = "list") {
        sendMessage(getStats(subject, page))
    }
}