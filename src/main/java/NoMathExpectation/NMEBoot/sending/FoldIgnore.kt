package NoMathExpectation.NMEBoot.sending

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.message.data.*

@Serializable
@SerialName(FoldIgnore.Key.SERIAL_NAME)
object FoldIgnore : Message, MessageMetadata, ConstrainSingle {
    override fun contentToString() = ""

    override fun toString() = "[Folding Ignored]"

    override val key: MessageKey<*> get() = Key

    object Key : AbstractMessageKey<FoldIgnore>({ it.safeCast() }) {
        const val SERIAL_NAME = "FoldIgnore"
    }
}