package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.utils.adminPermission
import NoMathExpectation.NMEBoot.utils.plugin
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.nameCardOrNick

object CommandExport : CompositeCommand(
    plugin,
    "export",
    description = "导出数据",
    parentPermission = adminPermission
) {
    @SubCommand
    suspend fun group(group: Group, argsString: String = "") {
        val args = argsString.split("\\s".toRegex())
        var members = group.members;
        /*for (arg in args) {

        }*/

        val file = plugin.resolveDataFile("export_group_${group.id}.csv")
        val write = file.printWriter();
        write.use {
            write.write("id, name, level, joinTime\n")
            for (member in members) {
                val profile = member.queryProfile()
                write.write("${member.id}, ${member.nameCardOrNick}, ${profile.qLevel}, ${member.joinTimestamp}\n")
            }
        }
    }
}