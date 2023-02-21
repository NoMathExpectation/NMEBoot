package NoMathExpectation.NMEBoot.inventory

import NoMathExpectation.NMEBoot.utils.FixedDelayUseCounter
import NoMathExpectation.NMEBoot.utils.FixedRateUseCounter
import NoMathExpectation.NMEBoot.utils.UseCounter
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.PluginDataExtensions.withDefault
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import kotlin.time.Duration

@OptIn(ConsoleExperimentalApi::class)
object UseCounters : AutoSavePluginData("useCounters") {
    val checkin by value<Map<Long, UseCounter>>().withDefault { FixedRateUseCounter.ofDay(1) }
    val extraCheckin by value<Map<Long, UseCounter>>().withDefault { FixedRateUseCounter.ofDay(1) }
    val pull by value<Map<Long, UseCounter>>().withDefault { FixedDelayUseCounter(1, Duration.parse("11h30m")) }
}