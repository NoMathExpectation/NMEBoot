package NoMathExpectation.NMEBoot.RDLounge.cardSystem

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object CardSystemData : AutoSavePluginData("cardSystem") {
    var user: Set<ByteArray> by value()
    var box: List<Byte> by value()
}