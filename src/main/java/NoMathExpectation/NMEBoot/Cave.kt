package NoMathExpectation.NMEBoot

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginData
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.buildMessageChain

object Cave: AutoSavePluginData("cave") {
    private val messages: MutableList<MessageChain> by value()

    init {
        reload {
            plugin.reloadPluginData(this)
        }
    }

    fun add(message: MessageChain) = messages.add(message)

    fun getAndRemoveRandom() = messages.randomOrNull()?.also { messages.remove(it) }

    fun showAll() = buildMessageChain { messages.forEachIndexed() { index, msg ->
        + "$index. "
        + msg
        + "\n"
    } }

    fun remove(index: Int) = messages.removeAt(index)

    fun clear() = messages.clear()

    fun getHelp(isAdmin: Boolean) = buildString {
        appendLine("//cave...")
        appendLine("put ... : 放入一条留言")
        appendLine("read : 读取并销毁一条留言")

        if (isAdmin) {
            appendLine("show : 显示所有留言")
            appendLine("remove <i> : 移除第i条留言")
            appendLine("clear : 清空所有留言")
        }
    }
}