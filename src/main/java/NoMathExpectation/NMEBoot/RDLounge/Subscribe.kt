package NoMathExpectation.NMEBoot.RDLounge

import NoMathExpectation.NMEBoot.Main
import NoMathExpectation.NMEBoot.commandSystem.NormalUserStats
import NoMathExpectation.NMEBoot.commandSystem.NyanMilkSupplier
import NoMathExpectation.NMEBoot.commandSystem.RDLounge
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import java.time.LocalDateTime

object Subscribe {
    private const val id = NyanMilkSupplier.GROUP_ID

    fun subscribeChatRank() = Main.INSTANCE.launch {
        while (isActive) {
            val time = LocalDateTime.now()
            if (time.hour == 23 && time.minute == 59) {
                val contact = Bot.instances[0].getGroup(id) ?: continue
                contact.sendMessage(NormalUserStats.getMessageCountAndDailyAsForwardMessage(contact))
                delay(60000L)
            }
            delay(10000L)
        }
    }
}