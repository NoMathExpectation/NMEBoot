package NoMathExpectation.NMEBoot.RDLounge.cardSystem

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginData

object CardSystemData : AutoSavePluginData("cardSystem") {
    var user: Set<ByteArray> by value()
    var box: List<Byte> by value()

    init {
        reload {
            plugin.reloadPluginData(this)
        }
    }
}