package NoMathExpectation.NMEBoot.RDLounge.rhythmCafe

import NoMathExpectation.NMEBoot.FileUtils
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.buildMessageChain

object RhythmCafeSearchEngine {
    private const val apiKey = "nicolebestgirl"
    private val httpClient = HttpClient(CIO) {
        install(Resources)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
        defaultRequest {
            url("https://api.rhythm.cafe")
            header("x-typesense-api-key", apiKey)
        }
    }

    private lateinit var currentRequest: Request
    private lateinit var currentSearch: Result

    private suspend fun sendRequest(request: Request) {
        currentRequest = request
        currentSearch = httpClient.get(currentRequest).body()
    }

    @JvmBlockingBridge
    suspend fun search(query: String?): String {
        return try {
            sendRequest(Request(q = query ?: ""))
            toString()
        } catch (e: HttpRequestTimeoutException) {
            "请求超时"
        }
    }

    @JvmBlockingBridge
    suspend fun pageTo(page: Int): String {
        return try {
            sendRequest(currentRequest.copy(page = page))
            toString()
        } catch (e: HttpRequestTimeoutException) {
            "请求超时"
        }
    }

    fun getLink(index: Int) = currentSearch.hits[index - 1].document.url

    fun getLink2(index: Int) = currentSearch.hits[index - 1].document.url2

    @JvmBlockingBridge
    suspend fun downloadAndUpload(group: Group, index: Int) =
        FileUtils.uploadFile(group, FileUtils.download(getLink(index)))

    fun isSearched() = ::currentSearch.isInitialized

    override fun toString() = buildString {
        append("搜索结果:\n")
        append("找到${currentSearch.found}个谱面，第${currentSearch.page}页，共${(currentSearch.found - 1) / currentSearch.request_params.per_page + 1}页\n")

        for (matchedLevelIndex in currentSearch.hits.indices) {
            val matchedLevel = currentSearch.hits[matchedLevelIndex]

            append(matchedLevelIndex + 1)
            append(". ${matchedLevel.document.peerReviewed()}\n")
            append(matchedLevel.document.song)
            append("\n作者: ")
            append(matchedLevel.document.authors.joinToString())
            append("\n")
        }
    }

    @JvmBlockingBridge
    suspend fun getDescription(contact: Contact, index: Int) = buildMessageChain {
        val level = currentSearch.hits[index - 1].document

        FileUtils.getDownloadStream(level.image).use {
            +contact.uploadImage(it)
        }
        +"\n"

        +"歌曲名: ${level.song}\n"

        +"作曲家: ${level.artist}\n"

        +"作者: ${level.authors.joinToString()}\n"

        +"难度: ${level.getDifficulty()}\n"

        if (level.seizure_warning) {
            +"癫痫警告!\n"
        }

        +"同行评审: ${level.peerReviewed()}\n"

        +"描述:\n${level.description}\n"

        +"模式: "
        if (level.single_player) {
            +"1p "
        }
        if (level.two_player) {
            +"2p "
        }
        +"\n"

        +"标签: ${level.tags.joinToString()}"
    }

    fun sendHelp() = buildString {
        append("//chart...\n")
        append("help :显示此帮助\n")
        append("search <text> :搜索谱面\n")
        append("page <i> :将搜索结果翻到第i页\n")
        append("info <i> :显示当前页中第i个谱面的描述\n")
        append("link <i> :获取当前页中第i个谱面的链接\n")
        append("link2 <i> :获取当前页中第i个谱面的镜像链接\n")
        append("download <i> :下载当前页中第i个谱面")
    }
}