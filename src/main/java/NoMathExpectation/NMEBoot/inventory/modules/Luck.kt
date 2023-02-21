package NoMathExpectation.NMEBoot.inventory.modules

import NoMathExpectation.NMEBoot.utils.FixedRateUseCounter
import NoMathExpectation.NMEBoot.utils.UseCounter
import NoMathExpectation.NMEBoot.utils.plugin
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.PluginDataExtensions.withDefault
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginData
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import kotlin.random.Random

@Serializable
class Luck(var counter: UseCounter = FixedRateUseCounter.ofDay(1)) : Comparable<Luck> {
    var luck: Int = Random.nextInt(101)
        get() {
            if (counter.use()) {
                refreshLuck()
            }
            return field
        }

    fun refreshLuck() {
        luck = Random.nextInt(101)
    }

    override fun equals(other: Any?) = other is Luck && luck == other.luck

    override fun compareTo(other: Luck) = luck.compareTo(other.luck)

    override fun hashCode() = luck

    @OptIn(ConsoleExperimentalApi::class)
    companion object : AutoSavePluginData("luck") {
        val lucks by value<Map<Long, Luck>>().withDefault { Luck() }

        operator fun get(id: Long) = lucks[id]

        init {
            reload {
                plugin.reloadPluginData(this)
            }
        }
    }
}