package NoMathExpectation.NMEBoot

import NoMathExpectation.NMEBoot.utils.BpmOffsetAnalyzer
import NoMathExpectation.NMEBoot.utils.plugin
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import java.io.File

@OptIn(ConsoleExperimentalApi::class)
suspend fun main() {
    MiraiConsoleTerminalLoader.startAsDaemon()
    plugin.load()
    plugin.enable()

    val file = File("")
    val bpm = null
    val result = BpmOffsetAnalyzer.getBpmAndOffset(file, bpm)
    println(result)

    MiraiConsole.shutdown()
    MiraiConsole.job.join()
}