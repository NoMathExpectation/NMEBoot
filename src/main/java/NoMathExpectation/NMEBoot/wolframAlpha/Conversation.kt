package NoMathExpectation.NMEBoot.wolframAlpha

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.logger
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
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

        private val conversations: MutableMap<Long, Conversation> = HashMap()

        @JvmStatic
        operator fun get(id: Long) = conversations.computeIfAbsent(id) { Conversation() }
    }

    private var lastResult: Result? = null

    private val mutex = Mutex()

    private suspend fun query(query: Query) = mutex.withLock(query) {
        queryTimes++

        val response = if (lastResult == null || lastResult?.host == null) {
            HTTP_CLIENT.get(query)
        } else {
            HTTP_CLIENT.get(query) {
                url("https://${lastResult!!.host}/api/v1/conversation.jsp")
            }
        }

        lastResult = try {
            response.body()
        } catch (e: NoTransformationFoundException) {
            Result(error = response.bodyAsText())
        }

        lastResult!!
    }

    //@jvmblockingbridge
    suspend fun query(question: String): String {
        logger.info("开始请求wolfram api，问题：$question")
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