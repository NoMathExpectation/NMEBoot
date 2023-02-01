package NoMathExpectation.NMEBoot.utils.newYear

import NoMathExpectation.NMEBoot.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig

internal object NewYearConfig : AutoSavePluginConfig("new_year") {
    val newYear by value("2023-01-22T00:00:00Z")
    val groups: List<Long> by value()

    init {
        Main.INSTANCE.reloadPluginConfig(this)
    }

    private suspend fun send() = groups.map { Bot.instances[0].getGroup(it) }.forEach { it?.sendMessage("新年快乐！") }

    fun timedSend() = Main.INSTANCE.launch {
        while (isActive) {
            val now = Clock.System.now()
            val newYear = newYear.toInstant()

            if (now in newYear..(newYear.plus(1, DateTimeUnit.SECOND))) {
                send()
                break
            } else {
                delay(100)
            }
        }
    }
}