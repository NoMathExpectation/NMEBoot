package NoMathExpectation.NMEBoot.RDLounge.cardSystem

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginData

object StatsData : AutoSavePluginData("stats") {
    var itemPullCount: Int by value()
    var cardPullCount: Int by value()
    var prayCount: Int by value()
    var auctionSuccessCount: Int by value()
    var auctionFailCount: Int by value()
    var auctionErrorCount: Int by value()

    init {
        reload {
            plugin.reloadPluginData(this)
        }
    }
}