package NoMathExpectation.NMEBoot.commandSystem

import NoMathExpectation.NMEBoot.Main
import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.code.CodableMessage
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.SingleMessage
import net.mamoe.mirai.message.data.buildMessageChain

object Alias : AutoSavePluginConfig("alias") {
    private val replaces: MutableMap<Long, MutableList<Triple<String, String, Boolean>>> by value()
    private val logger = Main.INSTANCE.logger

    init {
        reload {
            plugin.reloadPluginConfig(this)
        }
    }

    fun String.alias(group: Long): String {
        logger.verbose("Alias: String incoming '$this'")

        val replaces = replaces[group] ?: return this
        var after = this

        //val job = Thread {
        //while (true) {
        //val before = after
        for (p in replaces) {
            val job = Thread {
                after = Regex(p.first).replace(after, p.second)
            }
            job.isDaemon = true
            job.start()
            job.join(1000)
            if (job.isAlive) {
                logger.warning("Alias: Skipped ${p.first} -> ${p.second} due to timed out.")
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
            logger.info("Alias: '$this' -> '$after'")
        }

        logger.verbose("Alias: String outgoing '$after'")
        return after
    }

    fun Message.alias(contact: Contact): Message {
        logger.verbose("Alias: Message incoming '${contentToString()}'")

        if (this is SingleMessage) {
            return (if (this is CodableMessage)
                serializeToMiraiCode().alias(contact.id).deserializeMiraiCode(contact)
            else this).also {
                logger.verbose("Alias: Message outgoing '${it.contentToString()}'")
            }
        }

        return buildMessageChain {
            val sb = StringBuilder()
            for (sm in this@alias as MessageChain) {
                if (sm is CodableMessage) {
                    sb.append(sm.serializeToMiraiCode())
                } else {
                    if (sb.isNotEmpty()) {
                        +sb.toString().alias(contact.id).deserializeMiraiCode(contact)
                        sb.clear()
                    }
                    +sm
                }
            }
            if (sb.isNotEmpty()) {
                +sb.toString().alias(contact.id).deserializeMiraiCode(contact)
            }
        }.also { logger.verbose("Alias: Message outgoing '${it.contentToString()}'") }
    }

    fun Contact.alias() = object : Contact by this {
        @JvmBlockingBridge
        override suspend fun sendMessage(message: Message) =
            this@alias.sendMessage(message.alias(this))

        @JvmBlockingBridge
        override suspend fun sendMessage(message: String) =
            this@alias.sendMessage(message.alias(id))
    }

    fun add(group: Long, p: Triple<String, String, Boolean>) {
        val replaces = replaces.computeIfAbsent(group) { ArrayList() }
        replaces.add(p)
        logger.info("Alias: Added '${p.first}' -> '${p.second}' at position ${replaces.size - 1}")
    }

    fun add(group: Long, p: Triple<String, String, Boolean>, pos: Int) {
        val replaces = replaces.computeIfAbsent(group) { ArrayList() }
        replaces.add(pos, p)
        logger.info("Alias: Added '${p.first}' -> '${p.second}' at position $pos")
    }

    fun remove(group: Long, pos: Int, forced: Boolean): Triple<String, String, Boolean> {
        val replaces = replaces.computeIfAbsent(group) { ArrayList() }
        if (!forced && replaces[pos].third) {
            throw IllegalStateException("被保护的别称")
        }
        val p = replaces.removeAt(pos)
        logger.info("Alias: Removed '${p.first}' -> '${p.second}' at position $pos")
        return p
    }

    fun protect(group: Long, pos: Int): Boolean {
        val replaces = replaces[group] ?: throw IndexOutOfBoundsException()
        val p = replaces.removeAt(pos)
        val pNew = Triple(p.first, p.second, !p.third)
        replaces.add(pos, pNew)
        if (pNew.third) {
            logger.info("Alias: Removed protection of '${p.first}' -> '${p.second}' at position $pos")
        } else {
            logger.info("Alias: Protected '${p.first}' -> '${p.second}' at position $pos")
        }
        return pNew.third
    }

    fun clear(group: Long) {
        replaces[group]?.clear()
        logger.info("Alias: Cleared all aliases")
    }

    fun move(group: Long, from: Int, to: Int) {
        val replaces = replaces.computeIfAbsent(group) { ArrayList() }
        val tmp = replaces.removeAt(from)
        replaces.add(to, tmp)
        logger.info("Alias: Moved '${tmp.first}' -> '${tmp.second}' from position $from to position $to")
    }

    fun sendHelp() = buildMessageChain {
        +"//alias ...\n"
        +"help :显示此帮助\n"
        +"show :显示全部别称\n"
        +"add '<regex>' '<regex>' [pos] :添加新别称，使用\\'来逃避\n"
        +"remove <pos> :移除一条别称\n"
        +"move <from> <to> :将一条别称移动到新的位置\n"
        +"protect <pos> :保护/取消保护别称\n"
        +"clear :清除所有别称"
    }

    fun toString(group: Long) = buildString {
        val replaces = replaces[group] ?: return@buildString
        for (i in replaces.indices) {
            append("$i. ${replaces[i].first} -> ${replaces[i].second}\n")
        }
    }

    fun show(group: Long, pos: Int) = replaces.computeIfAbsent(group) { ArrayList() }[pos]
}