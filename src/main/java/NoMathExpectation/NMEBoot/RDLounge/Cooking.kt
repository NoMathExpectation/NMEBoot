package NoMathExpectation.NMEBoot.RDLounge

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.MessageChainBuilder

object Cooking : AutoSavePluginConfig("cooking") {
    private val condition by value("1 = 1")

    fun MessageChainBuilder.generate(contact: Contact?) {

    }
}