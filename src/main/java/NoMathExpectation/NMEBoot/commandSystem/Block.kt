package NoMathExpectation.NMEBoot.commandSystem

import NoMathExpectation.NMEBoot.Main
import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.buildMessageChain

object Block: AutoSavePluginConfig("block") {
    private val blocks: MutableMap<Long, MutableMap<Long, MutableList<String>>> by value()
    private val logger = Main.INSTANCE.logger

    init {
        reload {
            plugin.reloadPluginConfig(this)
        }
    }

    fun add(id: Long, target: Long, regex: String) {
        blocks.computeIfAbsent(id) { HashMap() }.computeIfAbsent(target) { ArrayList() }.add(regex)
        logger.info("Block: Added $regex for $target in $id.")
    }

    fun remove(id: Long, target: Long, index: Int): String {
        val regex = blocks.computeIfAbsent(id) { HashMap() }.computeIfAbsent(target) { ArrayList() }.removeAt(index)
        logger.info("Block: Removed $regex for $target in $id.")
        return regex
    }

    fun toString(group: Long) = buildMessageChain {
        append("成员被限制的指令：\n")
        val blocks = blocks[group] ?: return@buildMessageChain
        for (i in blocks.keys) {
            val memberBlocks = blocks[i]!!

            if (memberBlocks.isEmpty()) {
                continue
            }

            append(At(i))
            append(": \n")

            for (j in memberBlocks.indices) {
                append("$j. ${memberBlocks[j]}\n")
            }

            append("\n")
        }
    }

    fun toString(group: Long, target: Long) = buildMessageChain {
        append(At(target))
        append("被限制的指令：\n")
        val memberBlocks = blocks[group]?.get(target) ?: return@buildMessageChain
        for (i in memberBlocks.indices) {
            append("$i. ${memberBlocks[i]}\n")
        }
    }

    fun checkBlocked(id: Long, target: Long, message: String): Boolean {
        val generalBlocked = blocks[id]?.get(0L)
        val memberBlocked = blocks[id]?.get(target)

        val blockList = ArrayList<String>()
        memberBlocked?.let { blockList.addAll(it) }
        generalBlocked?.let { blockList.addAll(it) }

        var blocked = false
        for (p in blockList) {
            val job = Thread {
                if (Regex(p).containsMatchIn(message)) {
                    logger.info("Block: Blocked $message sent by $target with $p in $id.")
                    blocked = true
                }
            }
            job.isDaemon = true
            job.start()
            job.join(1000)
            if (job.isAlive) {
                logger.warning("Block: Skipped $p due to timed out.")
            }

            if (blocked) {
                return true
            }
        }

        return false
    }

    fun String.checkBlocked(id: Long, target: Long) = checkBlocked(id, target, this)

    fun sendHelp() = buildMessageChain {
        + "//block ...\n"
        + "add <@成员|@qq号> '<regex>' :添加某成员限制使用的指令，使用\\'来逃避\n"
        + "remove <@成员|@qq号> <index> : 移除某成员某成员限制使用的指令\n"
        + "show [@成员|@qq号]: 展示所有被限制的指令"
    }
}