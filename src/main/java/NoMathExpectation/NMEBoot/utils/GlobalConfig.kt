package NoMathExpectation.NMEBoot.utils

import NoMathExpectation.NMEBoot.Main
import NoMathExpectation.NMEBoot.inventory.NormalUser
import NoMathExpectation.NMEBoot.inventory.modules.reload
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Message

internal object GlobalConfig : AutoSavePluginConfig("globalConfig") {
    val bots: List<Long> by value()

    init {
        reload {
            plugin.reloadPluginConfig(this)
        }
    }
}

internal val plugin: Main by lazy { Main.INSTANCE }

internal val logger by lazy { plugin.logger }

internal val bots get() = GlobalConfig.bots.mapNotNull { Bot.getInstanceOrNull(it) }

internal fun getFriend(id: Long, bot: Long? = null) = bots.associateWith { it.getFriend(id) }
    .mapKeys { it.key.id }
    .let { if (bot == null) it.values.firstOrNull() else it[bot] ?: it.values.firstOrNull() }

internal fun getGroup(id: Long, bot: Long? = null) = bots.associateWith { it.getGroup(id) }
    .mapKeys { it.key.id }
    .let { if (bot == null) it.values.firstOrNull() else it[bot] ?: it.values.firstOrNull() }

internal fun getStranger(id: Long, bot: Long? = null) = bots.associateWith { it.getStranger(id) }
    .mapKeys { it.key.id }
    .let { if (bot == null) it.values.firstOrNull() else it[bot] ?: it.values.firstOrNull() }

internal fun Contact.launchSendMessage(s: String) = plugin.launch { sendMessage(s) }

internal fun Contact.launchSendMessage(message: Message) = plugin.launch { sendMessage(message) }

internal fun Contact.asyncSendMessage(s: String) = plugin.async { sendMessage(s) }

internal fun Contact.asyncSendMessage(message: Message) = plugin.async { sendMessage(message) }

internal fun NormalUser?.getRecentActiveContact() = if (this != null) RecentActiveContact[this] else null

internal suspend fun NormalUser.sendMessage(s: String) = getRecentActiveContact()?.sendMessage(s)

internal suspend fun NormalUser.sendMessage(message: Message) = getRecentActiveContact()?.sendMessage(message)

internal fun NormalUser.launchSendMessage(s: String) = getRecentActiveContact()?.launchSendMessage(s)

internal fun NormalUser.launchSendMessage(message: Message) = getRecentActiveContact()?.launchSendMessage(message)

internal fun NormalUser.asyncSendMessage(s: String) = getRecentActiveContact()?.asyncSendMessage(s)

internal fun NormalUser.asyncSendMessage(message: Message) = getRecentActiveContact()?.asyncSendMessage(message)