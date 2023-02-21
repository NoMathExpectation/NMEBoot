package NoMathExpectation.NMEBoot.wolframAlpha

import NoMathExpectation.NMEBoot.Main
import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig

class Conversation private constructor() {
    companion object : AutoSavePluginConfig("wolframAlpha") {
        val HTTP_CLIENT = HttpClient(CIO) {
            install(Resources)
            install(ContentNegotiation) {
                json(Json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
            }
            defaultRequest {
                url("https://api.wolframalpha.com")
            }
        }

        private val APP_ID: String by value("")
        var queryTimes: Int by value()
            private set

        init {
            reload {
                plugin.reloadPluginConfig(this)
            }
        }

        private val logger = Main.INSTANCE.logger

        private val conversations: MutableMap<Long, Conversation> = HashMap()

        @JvmStatic
        operator fun get(id: Long) = conversations.computeIfAbsent(id) { Conversation() }
    }

    private var lastResult: Result? = null

    private suspend fun query(query: Query): Result {
        val response = if (lastResult == null) {
            HTTP_CLIENT.get(query)
        } else {
            HTTP_CLIENT.get(query) {
                url("https://${lastResult!!.host}/api")
            }
        }

        return try {
            response.body()
        } catch (e: NoTransformationFoundException) {
            Result(error = response.bodyAsText())
        }
    }

    @JvmBlockingBridge
    suspend fun query(question: String): String {
        logger.info("开始请求wolfram api，问题：$question")
        queryTimes++
        val result = query(Query(APP_ID, question, conversationid = lastResult?.conversationID, s = lastResult?.s))
        return if (result.error != null) {
            logger.warning("请求错误：${result.error}")
            result.error
        } else {
            logger.info("请求结束，结果：${result.result}")
            result.result!!
        }
    }
}