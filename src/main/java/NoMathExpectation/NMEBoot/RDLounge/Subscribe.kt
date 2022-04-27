package NoMathExpectation.NMEBoot.RDLounge

import NoMathExpectation.NMEBoot.commandSystem.NormalUserStats
import NoMathExpectation.NMEBoot.commandSystem.RDLounge
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import java.time.LocalDateTime

object Subscribe {
    private const val id = RDLounge.GROUP_ID

    fun subscribeChatRank() = runBlocking {
        launch {
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
}