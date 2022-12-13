package NoMathExpectation.NMEBoot.utils

import NoMathExpectation.NMEBoot.Main
import com.alibaba.fastjson2.JSONException
import com.github.plexpt.chatgpt.Chatbot
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig

object ChatGPT : AutoSavePluginConfig("ChatGPT") {
    private val token: String by value("Insert your token here.")
    private val cfClear: String by value("Insert your cf_clearance here.")
    private val userAgent: String by value("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36")
    private val instances: MutableMap<Long, Chatbot> = mutableMapOf()
    private val logger = Main.INSTANCE.logger

    init {
        Main.INSTANCE.reloadPluginConfig(this)
    }

    operator fun get(id: Long) = instances.getOrPut(id) { Chatbot(token, cfClear, userAgent) }

    private suspend fun chat0(id: Long, msg: String) = get(id).getChatResponse(msg)["message"].toString()

    @JvmBlockingBridge
    suspend fun chat(id: Long, msg: String) = try {
        logger.info("ChatGPT: Message from group $id: $msg")
        chat0(id, msg)
    } catch (e: JSONException) {
        refresh(id)
        chat0(id, msg)
    }

    fun rollback(id: Long) {
        instances[id]?.rollbackConversation()
        logger.info("ChatGPT: Rollback conversation for group $id.")
    }

    @JvmBlockingBridge
    suspend fun refresh(id: Long) {
        instances[id]?.refreshSession()
        logger.info("ChatGPT: Refreshed session for group $id.")
    }

    fun reset(id: Long) {
        instances[id]?.resetChat()
        logger.info("ChatGPT: Reset session for group $id.")
    }

    fun reload() {
        Main.INSTANCE.reloadPluginConfig(this)
        instances.clear()
        logger.info("ChatGPT: Reloaded config.")
    }

    fun getHelp() = buildString {
        appendLine("//chat ...")
        appendLine("help :展示此帮助")
        appendLine("send <message> :发送一条消息")
        appendLine("rollback :撤回上一条消息")
        appendLine("reset :重置会话")
        appendLine()
        appendLine("由于网络延迟，请求需要一定时间才能回复，请耐心等待。")
        appendLine()
        appendLine("警告：本指令使用第三方API，任何使用的后果请自行承担。")
    }
}