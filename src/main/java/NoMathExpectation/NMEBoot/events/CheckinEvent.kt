package NoMathExpectation.NMEBoot.events

import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.AbstractEvent
import net.mamoe.mirai.event.CancellableEvent
import net.mamoe.mirai.event.events.MessageEvent

data class CheckinEvent(val user: User, val origin: MessageEvent) : AbstractEvent(), CancellableEvent
