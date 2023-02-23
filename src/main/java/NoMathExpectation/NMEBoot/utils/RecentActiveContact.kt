package NoMathExpectation.NMEBoot.utils

import NoMathExpectation.NMEBoot.inventory.NormalUser
import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent

internal object RecentActiveContact {
    val map: MutableMap<Long, Contact?> = mutableMapOf()

    operator fun set(id: Long, contact: Contact?) {
        map[id] = contact
    }

    operator fun get(id: Long) = map[id]

    operator fun get(normalUser: NormalUser) = get(normalUser.id)

    @JvmName("startListening")
    internal fun startListening() =
        GlobalEventChannel.parentScope(plugin).subscribeAlways<MessageEvent>(priority = EventPriority.HIGHEST) {
            if (toCommandSender().hasUsePermission()) {
                map[sender.id] = subject
            }
        }
}