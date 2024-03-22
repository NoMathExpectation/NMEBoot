package NoMathExpectation.NMEBoot.inventory.card

import NoMathExpectation.NMEBoot.inventory.Pool
import NoMathExpectation.NMEBoot.inventory.card.rdTradingCards.CardGroup
import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.inventory.registerItem
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.reloadAsJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.PluginDataExtensions.withEmptyDefault
import net.mamoe.mirai.console.data.value
import java.io.File
import java.util.*

@Serializable
class CardRepository private constructor(
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

        private val library: MutableMap<String, Card> = mutableMapOf() // id -> Card

        private val repos: MutableMap<String, CardRepository> by value() // id -> CardRepository

        @JvmStatic
        //@jvmblockingbridge
        suspend fun reloadRepositories() {
            repos.values.forEach {
                try {
                    it.sync()
                } catch (e: Exception) {
                    logger.error(e)
                }
                it.reload()
            }
        }

        suspend fun addRepository(gitRepository: String, group: Long? = null) =
            (repos.values.firstOrNull { it.gitRepository == gitRepository }
                ?: CardRepository(gitRepository)).apply {
                sync()
                reload()
                repos[id] = this
                if (group != null) groupRepos[group].add(id)
            }

        fun removeRepository(id: String, group: Long? = null) {
            if (group == null) {
                groupRepos.values.forEach { it -= id }
                repos -= id
            } else {
                groupRepos[group] -= id
                if (groupRepos.values.none { id in it }) {
                    repos -= id
                }
            }
        }

        fun getRepositories(group: Long) = groupRepos[group].mapNotNull { repos[it] }

        fun getPool(group: Long) = groupRepos[group].mapNotNull { repos[it]?.cardGroups }.run {
            val result = Pool<CardGroup>()
            forEach { result.addAll(it) }
            result
        }

        private val groupRepos by value<MutableMap<Long, MutableSet<String>>>().withEmptyDefault() // group -> repos.id

        operator fun get(id: String) = library[id]

        private val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        init {
            reload {
                reloadAsJson()
            }
        }
    }

    suspend fun sync() {
        logger.debug("卡牌库 $gitRepository 开始同步")
        val path = File(storePath)
        withContext(Dispatchers.IO) {
            if (path.isDirectory) {
                val git = ProcessBuilder("git", "pull").directory(path).start()
                git.waitFor()

                check(git.exitValue() == 0) {
                    "卡牌库同步：拉取 $gitRepository 的时候产生了一个错误：\n${
                        git.errorStream.bufferedReader().readText()
                    }"
                }
                logger.verbose(git.inputStream.bufferedReader().readText())
            } else {
                val git = Runtime.getRuntime().exec(arrayOf("git", "clone", gitRepository, storePath))
                git.waitFor()

                check(git.exitValue() == 0) {
                    "卡牌库同步：克隆 $gitRepository 的时候产生了一个错误：\n${
                        git.errorStream.bufferedReader().readText()
                    }"
                }
                logger.verbose(git.inputStream.bufferedReader().readText())
            }
        }

        logger.debug("卡牌库 $gitRepository 同步完成")
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun reload() {
        logger.debug("卡牌库：开始重载 $gitRepository")
        val dataPath = File(storePath)
        check(dataPath.isDirectory) { "卡牌库：无法找到卡牌数据文件夹" }

        cardGroups.flatMap { it.cards }.forEach { library.remove(it.id) }
        cardGroups.clear()

        withContext(Dispatchers.IO) {
            //read setting
            setting = try {
                File("$storePath/setting.json").inputStream().use { json.decodeFromStream(it) }
            } catch (exception: Exception) {
                logger.warning("卡牌库：无法读取 $gitRepository 的配置文件，将使用默认配置")
                logger.debug(exception)
                CardRepositorySetting()
            }

            //read cards
            File("$storePath/${setting.dataPath}").walkTopDown().filter { it.name.endsWith(".json") }.forEach { file ->
                try {
                    val cardGroup = file.inputStream().use { stream ->
                        json.decodeFromStream<CardGroup>(stream)
                    }
                    cardGroup.setRepository(this@CardRepository)
                    cardGroup.cards.forEach { registerItem(it) }
                    cardGroup.cards.associateBy { card -> card.id }.let { map -> library.putAll(map) }
                    cardGroups.add(cardGroup)
                } catch (e: Exception) {
                    logger.warning("卡牌库：在解析 ${file.name} 的时候出现了一个错误：")
                    logger.warning(e)
                }
            }

            //read assets
            filenameToFileMap = File("$storePath/${setting.assetPath}")
                .walkTopDown()
                .associateBy { it.nameWithoutExtension }
        }

        logger.debug("卡牌库：重载 $gitRepository 完成，共加载了 ${cardGroups.size} 个卡组，${cardGroups.sumOf { it.cards.size }} 个卡牌")
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