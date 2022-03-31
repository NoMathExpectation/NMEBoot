package NoMathExpectation.NMEBoot.commandSystem

import NoMathExpectation.NMEBoot.Main
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig

object UsingGroup : AutoSavePluginConfig("usingGroup") {
    val group: Set<Long> by value()

    init {
        Main.INSTANCE.reloadPluginConfig(this)
    }
}