package NoMathExpectation.NMEBoot.commandSystem

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginData


object Samurai : AutoSavePluginData("samurai") {
    var samurai: Boolean by value()
    var lastSamuraiWord: String by value("")

    init {
        reload {
            plugin.reloadPluginData(this)
        }
    }
}