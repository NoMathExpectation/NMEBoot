package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.RDLounge.rhythmCafe.Request
import NoMathExpectation.NMEBoot.RDLounge.rhythmCafe.RhythmCafeSearchEngine
import NoMathExpectation.NMEBoot.commandSystem.services.RDLoungeIntegrated
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.rdlPermission
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.MemberCommandSender

object CommandChart : CompositeCommand(
    plugin,
    "chart",
    description = "搜索谱面",
    parentPermission = rdlPermission
) {
    @SubCommand
    @Description("帮助")
    suspend fun CommandSender.help() {
        sendMessage(RhythmCafeSearchEngine.sendHelp())
    }

    @SubCommand
    @Description("搜索谱面")
    suspend fun CommandSender.search(keyword: String = "", itemPerPage: Int = 10, peerReview: Boolean = false) {
        sendMessage(RhythmCafeSearchEngine.search(keyword, itemPerPage, peerReview))
    }

    @SubCommand
    @Description("翻页")
    suspend fun CommandSender.page(page: Int) {
        if (!RhythmCafeSearchEngine.isSearched()) {
            sendMessage("请先进行一次搜索。")
            return
        }
        if (page < 1) {
            sendMessage("页码不能小于1。")
            return
        }

        sendMessage(RhythmCafeSearchEngine.pageTo(page))
    }

    @SubCommand
    @Description("获取谱面介绍")
    suspend fun CommandSender.info(index: Int) {
        if (!RhythmCafeSearchEngine.isSearched()) {
            sendMessage("请先进行一次搜索。")
            return
        }
        if (index !in 1..RhythmCafeSearchEngine.itemPerPage) {
            sendMessage("索引超出范围。")
            return
        }

        sendMessage(RhythmCafeSearchEngine.getDescription(index, subject))
    }

    @SubCommand
    @Description("获取谱面下载链接")
    suspend fun CommandSender.link(index: Int) {
        if (!RhythmCafeSearchEngine.isSearched()) {
            sendMessage("请先进行一次搜索。")
            return
        }
        if (index !in 1..RhythmCafeSearchEngine.itemPerPage) {
            sendMessage("索引超出范围。")
            return
        }

        sendMessage(RhythmCafeSearchEngine.getLink(index))
    }

    @SubCommand
    @Description("获取谱面镜像下载链接")
    suspend fun CommandSender.link2(index: Int) {
        if (!RhythmCafeSearchEngine.isSearched()) {
            sendMessage("请先进行一次搜索。")
            return
        }
        if (index !in 1..RhythmCafeSearchEngine.itemPerPage) {
            sendMessage("索引超出范围。")
            return
        }

        sendMessage(RhythmCafeSearchEngine.getLink2(index))
    }

    @SubCommand
    @Description("下载并上传谱面至群文件")
    suspend fun MemberCommandSender.download(index: Int) {
        if (group.id == RDLoungeIntegrated.RDLOUNGE) {
            return
        }

        if (!RhythmCafeSearchEngine.isSearched()) {
            sendMessage("请先进行一次搜索。")
            return
        }
        if (index !in 1..RhythmCafeSearchEngine.itemPerPage) {
            sendMessage("索引超出范围。")
            return
        }

        try {
            RhythmCafeSearchEngine.downloadAndUpload(subject, index)
        } catch (e: Exception) {
            sendMessage("传输失败")
        }
    }

    @SubCommand
    @Description("获取待定谱面数")
    suspend fun CommandSender.pending() {
        kotlin.runCatching {
            val count = RhythmCafeSearchEngine.getPendingLevelCount()
            val countStr = if (count >= Request.MAX_PER_PAGE) "${count - 1}+" else count
            sendMessage("待定谱面数：$countStr")
        }.onFailure {
            sendMessage("请求失败")
        }
    }
}