package NoMathExpectation.NMEBoot.commandSystem

import NoMathExpectation.NMEBoot.Main
import me.him188.kotlin.jvm.blocking.bridge.JvmBlockingBridge
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.*

object Alias: AutoSavePluginConfig("alias") {
    private val replaces: MutableMap<Long, MutableList<Pair<String, String>>> by value()
    private val protected: MutableMap<Long, MutableSet<Int>> by value()
    private val logger = Main.INSTANCE.logger

    init {
        Main.INSTANCE.reloadPluginConfig(this)
    }

    fun String.alias(group: Long): String {
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
        return after
    }

    fun Message.alias(group: Long): Message {
        if (this is SingleMessage) {
            if (this is PlainText) {
                return PlainText(content.alias(group))
            }

            return this
        }

        return buildMessageChain {
            for (sm in this@alias as MessageChain) {
                if (sm is PlainText) {
                    + sm.content.alias(group)
                } else {
                    + sm
                }
            }
        }
    }

    fun Contact.alias() = object: Contact by this {
        @JvmBlockingBridge
        override suspend fun sendMessage(message: Message) =
            this@alias.sendMessage(message.alias(id))

        @JvmBlockingBridge
        override suspend fun sendMessage(message: String) =
            this@alias.sendMessage(message.alias(id))
    }

    fun add(group: Long, p: Pair<String, String>) {
        val replaces = replaces.computeIfAbsent(group) { ArrayList() }
        replaces.add(p)
        logger.info("Alias: Added '${p.first}' -> '${p.second}' at position ${replaces.size - 1}")
    }

    fun add(group: Long, p: Pair<String, String>, pos: Int) {
        val replaces = replaces.computeIfAbsent(group) { ArrayList() }
        replaces.add(pos, p)
        logger.info("Alias: Added '${p.first}' -> '${p.second}' at position $pos")
    }

    fun remove(group: Long, pos: Int, forced: Boolean): Pair<String, String> {
        val protected = protected.computeIfAbsent(group) {HashSet()}
        if (!forced && protected.contains(pos)) {
            throw IllegalStateException("被保护的别称")
        }
        val replaces = replaces.computeIfAbsent(group) { ArrayList() }
        val p = replaces.removeAt(pos)
        logger.info("Alias: Removed '${p.first}' -> '${p.second}' at position $pos")
        return p
    }

    fun protect(group: Long, pos: Int): Boolean {
        val replaces = replaces[group]?: throw IndexOutOfBoundsException()
        val protected = protected.computeIfAbsent(group) {HashSet()}
        val p = replaces[pos]
        if (protected.contains(pos)) {
            protected.remove(pos)
            logger.info("Alias: Removed protection of '${p.first}' -> '${p.second}' at position $pos")
        } else {
            protected.add(pos)
            logger.info("Alias: Protected '${p.first}' -> '${p.second}' at position $pos")
        }
        return protected.contains(pos)
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
        + "//alias ...\n"
        + "help :显示此帮助\n"
        + "show :显示全部别称\n"
        + "add '<regex>' '<regex>' [pos] :添加新别称，使用\\'来逃避\n"
        + "remove <pos> :移除一条别称\n"
        + "move <from> <to> :将一条别称移动到新的位置\n"
        + "protect <pos> :保护/取消保护别称"
    }

    fun toString(group: Long) = buildString {
        val replaces = replaces[group] ?: return@buildString
        for (i in replaces.indices) {
            append("$i. ${replaces[i].first} -> ${replaces[i].second}\n")
        }
    }

    fun show(group: Long, pos: Int) = replaces.computeIfAbsent(group) { ArrayList() }[pos]
}