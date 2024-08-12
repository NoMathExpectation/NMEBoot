package NoMathExpectation.NMEBoot

import NoMathExpectation.NMEBoot.commands.InterceptingCommandCall
import NoMathExpectation.NMEBoot.commands.registerCommands
import NoMathExpectation.NMEBoot.inventory.card.CardRepository
import NoMathExpectation.NMEBoot.inventory.registerAllItems
import NoMathExpectation.NMEBoot.sending.inspectSendEvents
import NoMathExpectation.NMEBoot.simbot.SimConfig
import NoMathExpectation.NMEBoot.utils.*
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.descriptor.ExperimentalCommandDescriptors
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import java.io.File

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        "NoMathExpectation.NMEBoot",
        "1.4.2-2024052502",
        "NMEBoot"
    ) {
        author("NoMathExpectation")
    }
) {
    val wordle: WordleMirai = WordleMirai(File("config/NoMathExpectation.NMEBoot/wordle.txt"), 6, 25)

    @OptIn(ExperimentalCommandDescriptors::class)
    override fun PluginComponentStorage.onLoad() {
        contributeCommandCallParser(InterceptingCommandCall)
    }

    override fun onEnable() {
        super.onEnable()

        registerAllItems()

        runBlocking {
            CardRepository.reloadRepositories()
        }

        registerPermissions()
        registerCommands()

        RecentActiveContact.startListening()

        inspectSendEvents()

        DatabaseConfig.load()
        MessageHistory.recordStart()
        nudgeForRandomMessage()
        Repeat.startMonitor()

        AutoFileRemove.launchRoutine()

        SimConfig.start()

        logger.info("NMEBoot已加载。")
    }

    override fun onDisable() {
        super.onDisable()

        ktorClient.close()
        SimConfig.stop()
        logger.info("NMEBoot已停用。")
    }
}