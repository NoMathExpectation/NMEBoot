package NoMathExpectation.NMEBoot.utils

import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotEvent

val botEventChannel = GlobalEventChannel.parentScope(plugin).filter { it is BotEvent && it.bot.id in GlobalConfig.bots }