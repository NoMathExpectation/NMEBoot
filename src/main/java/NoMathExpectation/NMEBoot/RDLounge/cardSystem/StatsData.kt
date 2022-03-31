package NoMathExpectation.NMEBoot.RDLounge.cardSystem

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object StatsData : AutoSavePluginData("stats") {
    var itemPullCount: Int by value()
    var cardPullCount: Int by value()
    var prayCount: Int by value()
    var auctionSuccessCount: Int by value()
    var auctionFailCount: Int by value()
    var auctionErrorCount: Int by value()
}