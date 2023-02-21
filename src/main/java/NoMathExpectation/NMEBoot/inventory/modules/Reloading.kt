package NoMathExpectation.NMEBoot.inventory.modules

import net.mamoe.mirai.console.data.PluginData
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@OptIn(ConsoleExperimentalApi::class)
object Reloading {
    private val reloadFunctions: MutableMap<String, () -> Unit> = mutableMapOf()

    operator fun set(pluginData: PluginData, reloadFunction: () -> Unit) {
        reloadFunctions[pluginData.saveName] = reloadFunction
    }

    fun reload(saveName: String): Boolean = kotlin.runCatching {
        reloadFunctions[saveName]?.invoke() ?: return false
        return true
    }.getOrDefault(false)
}

fun PluginData.reload(reloadFunction: () -> Unit) {
    reloadFunction()
    Reloading[this] = reloadFunction
}