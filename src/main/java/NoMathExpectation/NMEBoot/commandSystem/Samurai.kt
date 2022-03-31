package NoMathExpectation.NMEBoot.commandSystem

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value


object Samurai : AutoSavePluginData("samurai") {
    var samurai: Boolean by value()
    var lastSamuraiWord: String by value("")
}