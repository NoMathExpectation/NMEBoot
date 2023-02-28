package NoMathExpectation.NMEBoot.sending

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.message.data.*

@Serializable
@SerialName(AliasIgnore.Key.SERIAL_NAME)
object AliasIgnore : Message, MessageMetadata, ConstrainSingle {
    override fun contentToString() = ""

    override fun toString() = "[Alias Ignored]"

    override val key: MessageKey<*> get() = Key

    object Key : AbstractMessageKey<AliasIgnore>({ it.safeCast() }) {
        const val SERIAL_NAME = "AliasIgnore"
    }
}