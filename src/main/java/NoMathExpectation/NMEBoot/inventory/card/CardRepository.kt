package NoMathExpectation.NMEBoot.inventory.card

import NoMathExpectation.NMEBoot.inventory.Pool
import NoMathExpectation.NMEBoot.inventory.card.rdTradingCards.CardGroup
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import java.io.File
import java.util.*

@Serializable
class CardRepository(
    val gitRepository: String,
    @Required val id: String = UUID.randomUUID().toString(),
    @Required val storePath: String = "$defaultStorePath/$id",
) {
    @Transient
    lateinit var setting: CardRepositorySetting
        private set

    @Transient
    val cardGroups: Pool<CardGroup> = Pool()

    @Transient
    lateinit var filenameToFileMap: Map<String, File>
        private set

    companion object CardLibrary : AutoSavePluginConfig("cardLibrary") {
        val defaultStorePath by value("${plugin.dataFolder.absolutePath}/cardRepositories")

        private val library: MutableMap<String, Card> = mutableMapOf()

        operator fun get(id: String) = library[id]

        private val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    suspend fun sync() {
        logger.debug("卡牌库 $gitRepository 开始同步")
        val path = File(storePath)
        withContext(Dispatchers.IO) {
            if (path.isDirectory) {
                val git = ProcessBuilder("git", "pull").directory(path).start()
                git.waitFor()

                val error = git.errorStream.bufferedReader().readText()
                check(error.isBlank()) { "卡牌库同步：拉取 $gitRepository 的时候产生了一个错误：\n$error" }
            } else {
                val git = Runtime.getRuntime().exec(arrayOf("git", "clone", gitRepository, storePath))
                git.waitFor()

                val error = git.errorStream.bufferedReader().readText()
                check(error.isBlank()) { "卡牌库同步：克隆 $gitRepository 的时候产生了一个错误：\n$error" }
            }
        }

        logger.debug("卡牌库 $gitRepository 同步完成")
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun reload() {
        logger.debug("卡牌库同步：开始重载 $gitRepository")
        val dataPath = File(storePath)
        check(dataPath.isDirectory) { "卡牌库同步：无法找到卡牌数据文件夹" }

        cardGroups.flatMap { it.cards }.forEach { library.remove(it.id) }
        cardGroups.clear()

        withContext(Dispatchers.IO) {
            //read setting
            setting = try {
                File("$storePath/setting.json").inputStream().use { json.decodeFromStream(it) }
            } catch (exception: Exception) {
                logger.warning("卡牌库同步：无法读取 $gitRepository 的配置文件，将使用默认配置")
                logger.debug(exception)
                CardRepositorySetting()
            }

            //read cards
            File("$storePath/${setting.dataPath}").listFiles { dir, name -> dir.isFile && name.endsWith(".json") }
                ?.forEach {
                    val cardGroup = it.inputStream().use { stream ->
                        json.decodeFromStream<CardGroup>(stream)
                    }
                    cardGroup.setRepository(this@CardRepository)
                    cardGroup.cards.associateBy { card -> card.id }.let { map -> library.putAll(map) }
                    cardGroups.add(cardGroup)
                }

            //read assets
            filenameToFileMap = File("$storePath/${setting.assetPath}")
                .listFiles { dir, name -> dir.isFile }
                ?.associateBy { it.nameWithoutExtension }
                ?: emptyMap()
        }

        logger.debug("卡牌库同步：重载 $gitRepository 完成，共加载了 ${cardGroups.size} 个卡组，${cardGroups.sumOf { it.cards.size }} 个卡牌")
    }

    init {
        plugin.launch {
            sync()
            reload()
        }
    }

    override fun equals(other: Any?) = gitRepository == (other as? CardRepository)?.gitRepository

    override fun hashCode() = gitRepository.hashCode()

    override fun toString() = "CardRepository(gitRepository='$gitRepository')"
}

@Serializable
data class CardRepositorySetting(
    val dataPath: String = "data/cards",
    val assetPath: String = "card_images"
)