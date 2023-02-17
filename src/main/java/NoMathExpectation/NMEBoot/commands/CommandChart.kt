package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.RDLounge.rhythmCafe.RhythmCafeSearchEngine
import NoMathExpectation.NMEBoot.sending.asCustom
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
    @SubCommand()
    @Description("帮助")
    suspend fun CommandSender.help() = with(asCustom()) {
        sendMessage(RhythmCafeSearchEngine.sendHelp())
    }

    @SubCommand()
    @Description("搜索谱面")
    suspend fun CommandSender.search(keyword: String) = with(asCustom()) {
        sendMessage(RhythmCafeSearchEngine.search(keyword))
    }

    @SubCommand()
    @Description("翻页")
    suspend fun CommandSender.page(page: Int) = with(asCustom()) {
        if (!RhythmCafeSearchEngine.isSearched()) {
            sendMessage("请先进行一次搜索。")
            return@with
        }
        if (page < 1) {
            sendMessage("页码不能小于1。")
            return@with
        }

        sendMessage(RhythmCafeSearchEngine.pageTo(page))
    }

    @SubCommand()
    @Description("获取谱面介绍")
    suspend fun CommandSender.info(index: Int) = with(asCustom()) {
        if (!RhythmCafeSearchEngine.isSearched()) {
            sendMessage("请先进行一次搜索。")
            return@with
        }
        if (index !in 1..5) {
            sendMessage("索引超出范围。")
            return@with
        }

        sendMessage(RhythmCafeSearchEngine.getDescription(index, subject))
    }

    @SubCommand()
    @Description("获取谱面下载链接")
    suspend fun CommandSender.link(index: Int) = with(asCustom()) {
        if (!RhythmCafeSearchEngine.isSearched()) {
            sendMessage("请先进行一次搜索。")
            return@with
        }
        if (index !in 1..5) {
            sendMessage("索引超出范围。")
            return@with
        }

        sendMessage(RhythmCafeSearchEngine.getLink(index))
    }

    @SubCommand()
    @Description("获取谱面镜像下载链接")
    suspend fun CommandSender.link2(index: Int) = with(asCustom()) {
        if (!RhythmCafeSearchEngine.isSearched()) {
            sendMessage("请先进行一次搜索。")
            return@with
        }
        if (index !in 1..5) {
            sendMessage("索引超出范围。")
            return@with
        }

        sendMessage(RhythmCafeSearchEngine.getLink2(index))
    }

    @SubCommand()
    @Description("下载并上传谱面至群文件")
    suspend fun MemberCommandSender.download(index: Int) = with(asCustom()) {
        if (!RhythmCafeSearchEngine.isSearched()) {
            sendMessage("请先进行一次搜索。")
            return@with
        }
        if (index !in 1..5) {
            sendMessage("索引超出范围。")
            return@with
        }

        RhythmCafeSearchEngine.downloadAndUpload(origin.subject, index)
    }
}