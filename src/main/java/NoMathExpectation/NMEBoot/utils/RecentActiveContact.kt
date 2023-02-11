package NoMathExpectation.NMEBoot.utils

import NoMathExpectation.NMEBoot.inventory.NormalUser
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent

internal object RecentActiveContact {
    private val map: MutableMap<Long, Contact> = mutableMapOf()

    operator fun get(id: Long) = map[id]

    operator fun get(normalUser: NormalUser) = get(normalUser.id)

    @JvmName("startListening")
    internal fun startListening() =
        GlobalEventChannel.parentScope(plugin).subscribeAlways<MessageEvent>(priority = EventPriority.HIGHEST) {
            map[sender.id] = subject
        }
}