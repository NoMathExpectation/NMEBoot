package NoMathExpectation.NMEBoot.commandSystem

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.sending.AliasIgnore
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.plugin
import kotlinx.serialization.Serializable
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.PluginDataExtensions.withEmptyDefault
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.code.CodableMessage
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.*

@Serializable
data class AliasItem(
    var from: String,
    var to: String,
    var applyIn: Boolean = true,
    var applyOut: Boolean = true,
    var protected: Boolean = false
)

object Alias : AutoSavePluginConfig("alias") {
    private val replaces by value<MutableMap<Long, MutableList<AliasItem>>>().withEmptyDefault()

    init {
        reload {
            plugin.reloadPluginConfig(this)
        }
    }

    private fun String.alias(group: Long, filter: (AliasItem) -> Boolean): String {
        logger.verbose("Alias: String incoming '$this'")

        val replaces = replaces[group].filter(filter)
        var after = this

        //val job = Thread {
        //while (true) {
        //val before = after
        for (p in replaces) {
            val job = Thread {
                after = after.replace(p.from.toRegex(), p.to)
            }
            job.isDaemon = true
            job.start()
            job.join(1000)
            if (job.isAlive) {
                logger.warning("Alias: Skipped ${p.from} -> ${p.to} due to timed out.")
                if (after != this) {
                    logger.info("Alias: '$this' -> '$after'")
                }
            }
        }
        //if (before == after) {
        //break
        //}
        //}
        //}
        //job.isDaemon = true
        //job.start()
        //job.join(10000)
        //if (job.isAlive) {
        //logger.warning("Alias: Skipped remaining aliases due to timed out.")
        //}

        if (after != this) {
            logger.info("Alias: Result: '$this' -> '$after'")
        }

        logger.verbose("Alias: String outgoing '$after'")
        return after
    }

    fun String.alias(group: Long) = alias(group) { true }

    fun String.aliasIn(group: Long) = alias(group) { it.applyIn }

    fun String.aliasOut(group: Long) = alias(group) { it.applyOut }

    private fun Message.alias(contact: Contact, filter: (AliasItem) -> Boolean): Message {
        logger.verbose("Alias: Message incoming '$this'")

        if (this is AliasIgnore || this is MessageChain && this[AliasIgnore.Key] != null) {
            return also { logger.verbose("Alias: Message ignored outgoing '$it'") }
        }

        if (this is SingleMessage) {
            return when (this) {
                is ForwardMessage -> copy(nodeList = nodeList.map {
                    it.copy(
                        messageChain = it.messageChain.alias(contact, filter).toMessageChain()
                    )
                })

                is CodableMessage -> serializeToMiraiCode().alias(contact.id, filter).deserializeMiraiCode(contact)

                else -> this
            }.also {
                logger.verbose("Alias: Message outgoing '$it'")
            }
        }

        if (this is MessageChain && this[ForwardMessage.Key] != null) {
            return with(this[ForwardMessage]!!) {
                copy(nodeList = nodeList.map {
                    it.copy(
                        messageChain = it.messageChain.alias(contact, filter).toMessageChain()
                    )
                })
            }.also { logger.verbose("Alias: Message outgoing '$it'") }
        }

        return buildMessageChain {
            val sb = StringBuilder()
            for (sm in this@alias as MessageChain) {
                if (sm is CodableMessage) {
                    sb.append(sm.serializeToMiraiCode())
                } else {
                    if (sb.isNotEmpty()) {
                        +sb.toString().alias(contact.id, filter).deserializeMiraiCode(contact)
                        sb.clear()
                    }
                    +sm
                }
            }
            if (sb.isNotEmpty()) {
                +sb.toString().alias(contact.id, filter).deserializeMiraiCode(contact)
            }
        }.also { logger.verbose("Alias: Message outgoing '$it'") }
    }

    fun Message.alias(contact: Contact) = alias(contact) { true }

    fun Message.aliasIn(contact: Contact) = alias(contact) { it.applyIn }

    fun Message.aliasOut(contact: Contact) = alias(contact) { it.applyOut }

    fun Contact.alias() = object : Contact by this {
        @JvmBlockingBridge
        override suspend fun sendMessage(message: Message) =
            this@alias.sendMessage(message.alias(this))

        @JvmBlockingBridge
        override suspend fun sendMessage(message: String) =
            this@alias.sendMessage(message.alias(id))
    }

    fun add(group: Long, p: AliasItem) {
        val aliasItems = replaces[group]
        aliasItems.add(p)
        logger.info("Alias: Added '${p.from}' -> '${p.to}' at position ${aliasItems.size - 1}")
    }

    fun add(group: Long, p: AliasItem, pos: Int) {
        replaces[group].add(pos, p)
        logger.info("Alias: Added '${p.from}' -> '${p.to}' at position $pos")
    }

    fun remove(group: Long, pos: Int, forced: Boolean = false): AliasItem {
        val replaces = replaces[group]
        if (!forced && replaces[pos].protected) {
            throw IllegalStateException("被保护的别称")
        }
        val p = replaces.removeAt(pos)
        logger.info("Alias: Removed '${p.from}' -> '${p.to}' at position $pos")
        return p
    }

    fun protect(group: Long, pos: Int): Boolean {
        val replaces = replaces[group]
        val p = replaces[pos]
        p.protected = !p.protected
        if (p.protected) {
            logger.info("Alias: Removed protection of '${p.from}' -> '${p.to}' at position $pos")
        } else {
            logger.info("Alias: Protected '${p.from}' -> '${p.to}' at position $pos")
        }
        return p.protected
    }

    fun isProtected(group: Long, pos: Int) = replaces[group][pos].protected

    fun set(group: Long, pos: Int, apply: AliasItem.() -> Unit) = replaces[group][pos].apply(apply)

    fun clear(group: Long) {
        replaces[group].clear()
        logger.info("Alias: Cleared all aliases")
    }

    fun move(group: Long, from: Int, to: Int) {
        val replaces = replaces[group]
        val tmp = replaces.removeAt(from)
        replaces.add(to, tmp)
        logger.info("Alias: Moved '${tmp.from}' -> '${tmp.to}' from position $from to position $to")
    }

    fun sendHelp() = buildMessageChain {
        +"//alias ...\n"
        +"help :显示此帮助\n"
        +"show :显示全部别称\n"
        +"add \"<regex>\" \"<regex>\" [applyIn] [applyOut] [protected] [pos] :添加新别称\n"
        +"remove <pos> :移除一条别称\n"
        +"move <from> <to> :将一条别称移动到新的位置\n"
        +"protect <pos> :保护/取消保护别称\n"
        +"set <pos> [applyIn] [applyOut] [protected] :设置别称属性\n"
        +"clear :清除所有别称"
    }

    fun toString(group: Long) = buildString {
        val replaces = replaces[group]
        for (i in replaces.indices) {
            val aliasItem = replaces[i]
            append("$i. \"${aliasItem.from}\" -> \"${aliasItem.to}\"")
            if (aliasItem.applyIn) {
                append(" in")
            }
            if (aliasItem.applyOut) {
                append(" out")
            }
            if (aliasItem.protected) {
                append(" protected")
            }
            appendLine()
        }
    }

    fun show(group: Long, pos: Int) = replaces[group][pos]
}