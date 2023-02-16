package NoMathExpectation.NMEBoot.sending

import NoMathExpectation.NMEBoot.commandSystem.Alias.alias
import NoMathExpectation.NMEBoot.inventory.NormalUser
import NoMathExpectation.NMEBoot.inventory.toNormalUser
import NoMathExpectation.NMEBoot.utils.hasAdminPermission
import NoMathExpectation.NMEBoot.utils.plugin
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SystemCommandSender
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*

internal object SendingConfig : AutoSavePluginConfig("sending") {
    val enableAlias by value(true)
    val foldLength by value(150)
    val foldLineCount by value(10)
    val recallTime by value(-1L)

    init {
        plugin.reloadPluginConfig(this)
    }
}

fun Message.checkFold(): Boolean {
    if (this is ForwardMessage || (this is MessageChain && get(ForwardMessage.Key) != null)) {
        return false
    }

    val content = contentToString()
    return content.length > SendingConfig.foldLength || content.count { it == '\n' } > SendingConfig.foldLineCount
}

class CustomCommandSender<T : CommandSender>(val origin: T) : CommandSender by origin {
    override suspend fun sendMessage(message: Message): MessageReceipt<Contact>? {
        val aliasMessage = if (SendingConfig.enableAlias && subject != null) message.alias(subject) else message
        return if (origin !is SystemCommandSender && aliasMessage.checkFold()) {
            sendFoldMessage(aliasMessage)
        } else {
            origin.sendMessage(aliasMessage)
        }.also {
            if (SendingConfig.recallTime > 0) {
                it?.recallIn(SendingConfig.recallTime)
            }
        }
    }

    override suspend fun sendMessage(message: String) = sendMessage(PlainText(message))

    suspend fun sendFoldMessage(vararg messages: Message) = if (subject != null && bot != null) {
        sendMessage(buildForwardMessage(subject) {
            messages.forEach {
                bot says if (SendingConfig.enableAlias) it.alias(subject) else it
            }
        })
    } else {
        messages.forEach {
            sendMessage(it)
        }
        null
    }

    suspend fun sendFoldMessage(vararg messages: String) = if (subject != null && bot != null) {
        sendMessage(buildForwardMessage(subject) {
            messages.forEach {
                bot says if (SendingConfig.enableAlias) it.alias(subject.id) else it
            }
        })
    } else {
        messages.forEach {
            sendMessage(it)
        }
        null
    }
}

fun <T : CommandSender> T.asCustom() = CustomCommandSender(this)

class CustomContact<T : Contact>(val origin: T) : Contact by origin {
    override suspend fun sendMessage(message: Message): MessageReceipt<Contact> {
        val aliasMessage = if (SendingConfig.enableAlias) message.alias(origin) else message
        return if (aliasMessage.checkFold()) {
            sendFoldMessage(aliasMessage)
        } else {
            origin.sendMessage(aliasMessage)
        }.also {
            if (SendingConfig.recallTime > 0) {
                it.recallIn(SendingConfig.recallTime)
            }
        }
    }

    override suspend fun sendMessage(message: String) = sendMessage(PlainText(message))

    suspend fun sendFoldMessage(vararg messages: Message) = sendMessage(buildForwardMessage(origin) {
        messages.forEach {
            bot says if (SendingConfig.enableAlias) it.alias(origin) else it
        }
    })

    suspend fun sendFoldMessage(vararg messages: String) = sendMessage(buildForwardMessage(origin) {
        messages.forEach {
            bot says if (SendingConfig.enableAlias) it.alias(origin.id) else it
        }
    })
}

suspend fun CommandSender.checkAndGetNormalUser(overridden: NormalUser? = null): NormalUser? {
    return if (overridden != null) {
        if (hasAdminPermission()) {
            overridden
        } else {
            sendMessage("你没有权限操作他人的物品栏。")
            null
        }
    } else {
        user?.toNormalUser() ?: run {
            sendMessage("请提供要查询的对象。")
            null
        }
    }
}