package NoMathExpectation.NMEBoot.simbot

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.simbot.command.SimbotCommandManager
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.plugin
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking
import love.forte.simbot.application.listeners
import love.forte.simbot.component.kook.bot.KookBot
import love.forte.simbot.component.kook.kookBots
import love.forte.simbot.component.kook.useKook
import love.forte.simbot.core.application.SimpleApplication
import love.forte.simbot.core.application.launchSimpleApplication
import love.forte.simbot.event.Event
import love.forte.simbot.event.process
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig

object SimConfig : AutoSavePluginConfig("simbot") {
    private val kookId: String by value("")
    private val kookSecret: String by value("")

    internal var app: SimpleApplication? = null
        private set

    internal var kookBot: KookBot? = null
        private set

    internal suspend fun reload() {
        logger.info("重启simbot......")
        app?.cancel()

        app = null
        kookBot = null

        app = launchSimpleApplication {
            config {
                coroutineContext = plugin.coroutineContext
            }

            useKook()
        }

        app?.listeners {
            process<Event> {
                logger.info("simbot事件: $it")
            }
        }

        app?.kookBots {
            if (kookId.isNotBlank() && kookSecret.isNotBlank()) {
                kotlin.runCatching {
                    kookBot = registerWs(kookId, kookSecret) {
                        botConfiguration.clientEngineFactory = CIO
                        botConfiguration.wsEngineFactory = CIO
                    }.also { it.start() }
                }.onFailure {
                    logger.error(it)
                }
            }
        }

        SimbotCommandManager.register()
    }

    fun start() {
        reload {
            runBlocking {
                plugin.reloadPluginConfig(this@SimConfig)
                reload()
            }
        }
    }

    fun stop() {
        logger.info("关闭simbot......")
        app?.cancel()

        app = null
        kookBot = null
    }
}