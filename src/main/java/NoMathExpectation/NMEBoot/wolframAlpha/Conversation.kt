package NoMathExpectation.NMEBoot.wolframAlpha

import NoMathExpectation.NMEBoot.Main
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.runBlocking
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

class Conversation private constructor(){
    companion object: AutoSavePluginConfig("wolframAlpha") {
        val HTTP_CLIENT = HttpClient(Java) {
            install(Resources)
            defaultRequest {
                url("https://api.wolframalpha.com")
            }
        }

        private val APP_ID: String by value("")
        var queryTimes: Int by value()
            private set

        private val logger = Main.INSTANCE.logger

        private val conversations: MutableMap<Long, Conversation> = HashMap()
        operator fun get(id: Long) = conversations.computeIfAbsent(id) { Conversation() }
    }

    private var lastResult: Result? = null
    private val queryChannel: Channel<Query> = Channel(1)
    private val resultChannel = runBlocking {
        produce {
            while (true) {
                val query = queryChannel.receive()
                lastResult = if (lastResult == null) {
                    HTTP_CLIENT.get(query).body()
                } else {
                    HTTP_CLIENT.get(query) {
                        url("https://${lastResult!!.host}/api")
                    }.body()
                }
                send(lastResult!!)
            }
        }
    }

    @JvmBlockingBridge
    suspend fun query(question: String): String {
        logger.info("开始排队请求wolfram api，问题：$question")
        queryChannel.send(Query(APP_ID, question, conversationid = lastResult?.conversationID, s = lastResult?.s))
        val result = resultChannel.receive()
        queryTimes++
        if (result.error == null) {
            throw RuntimeException(result.error as String?)
        }
        logger.info("请求结束，结果：${result.result}")
        return result.result!!
    }
}