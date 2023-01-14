package NoMathExpectation.NMEBoot.RDLounge.NeoCardSystem.rdTradingCards

import NoMathExpectation.NMEBoot.RDLounge.NeoCardSystem.Card
import NoMathExpectation.NMEBoot.inventory.Pool
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import java.io.File

object CardLibrary : AutoSavePluginConfig("CardLibrary") {
    const val gitRepository = "https://github.com/DPS2004/rdtradingcards.git"
    val repositoryPath by value("${plugin.dataFolder.absolutePath}/rdtradingcards")

    val cardGroups: Pool<CardGroup> = Pool(mutableListOf())
    val library: MutableMap<String, Card> = mutableMapOf()

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun sync() {
        logger.info("rd卡牌库：开始同步")
        val path = File(repositoryPath)
        withContext(Dispatchers.IO) {
            if (path.isDirectory) {
                val git = ProcessBuilder("git", "pull").directory(path).start()
                git.waitFor()

                val error = git.errorStream.bufferedReader().readText()
                check(error.isBlank()) { "rd卡牌库同步：拉取远程 git 库的时候产生了一个错误：\n$error" }
            } else {
                val git = Runtime.getRuntime().exec(arrayOf("git", "clone", gitRepository, repositoryPath))
                git.waitFor()

                val error = git.errorStream.bufferedReader().readText()
                check(error.isBlank()) { "rd卡牌库同步：同步远程 git 库的时候产生了一个错误：\n$error" }
            }
        }

        logger.info("rd卡牌库：同步完成")
        reload()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun reload() {
        logger.info("rd卡牌库：开始重载")
        val dataPath = File("$repositoryPath/data/cards")
        check(dataPath.isDirectory) { "rd卡牌库：无法找到卡牌数据文件夹" }

        cardGroups.clear()
        library.clear()
        withContext(Dispatchers.IO) {
            dataPath.listFiles { dir, name -> dir.isFile && name.endsWith(".json") }?.forEach {
                val cardGroup = json.decodeFromStream<CardGroup>(it.inputStream())
                cardGroup.cards.associateBy { card -> card.id }.let { map -> library.putAll(map) }
                cardGroups.add(cardGroup)
            }
        }

        logger.info("rd卡牌库：重载完成，共加载了 ${cardGroups.size} 个卡组，${library.size} 个卡牌")
    }
}