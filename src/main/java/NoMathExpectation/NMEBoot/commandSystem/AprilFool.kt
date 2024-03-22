package NoMathExpectation.NMEBoot.commandSystem

import NoMathExpectation.NMEBoot.naptcha.CaptchaDispatcher
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Message

object AprilFool {
    fun getModifiedContact(contact: Contact) = object: Contact by contact {
        //@jvmblockingbridge
        override suspend fun sendMessage(message: Message) =
            contact.sendMessage(CaptchaDispatcher.obfuscate(message, 3))

        //@jvmblockingbridge
        override suspend fun sendMessage(message: String) =
            contact.sendMessage(CaptchaDispatcher.obfuscate(message, 3))
    }
}