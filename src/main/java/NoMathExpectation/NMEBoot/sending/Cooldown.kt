package NoMathExpectation.NMEBoot.sending

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.FixedDelayUseCounter
import NoMathExpectation.NMEBoot.utils.UseCounter
import NoMathExpectation.NMEBoot.utils.plugin
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.PluginDataExtensions.withDefault
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import kotlin.time.Duration

object Cooldown : AutoSavePluginConfig("cooldown") {
    private val cooldowns by value<MutableMap<Long, UseCounter>>().withDefault {
        FixedDelayUseCounter(
            1,
            Duration.parse("10s")
        )
    }

    init {
        reload {
            plugin.reloadPluginConfig(this)
        }
    }

    operator fun get(id: Long) = cooldowns[id]

    operator fun set(id: Long, counter: UseCounter) {
        cooldowns[id] = counter
    }

    fun isOnCooldown(id: Long) = !get(id).canUse()

    fun consumeCooldown(id: Long) = get(id).use()
}