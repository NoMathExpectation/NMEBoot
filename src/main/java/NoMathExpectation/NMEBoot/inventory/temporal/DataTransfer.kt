package NoMathExpectation.NMEBoot.inventory.temporal

import NoMathExpectation.NMEBoot.RDLounge.cardSystem.CardPool
import NoMathExpectation.NMEBoot.RDLounge.cardSystem.CardUser
import NoMathExpectation.NMEBoot.inventory.*
import NoMathExpectation.NMEBoot.inventory.Pool.Companion.box
import NoMathExpectation.NMEBoot.inventory.card.Card
import NoMathExpectation.NMEBoot.inventory.card.CardRepository
import NoMathExpectation.NMEBoot.utils.adminPermission
import NoMathExpectation.NMEBoot.utils.plugin
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.safeCast

internal val cardNameMapFile = plugin.resolveDataFile("card_map.txt")

internal val itemIdMap: Map<String, Item?> = cardNameMapFile
    .readLines()
    .map { it.split(":::") }
    .filter { it[1] != "null" }
    .associate<List<String>, String, Item?> {
        it[0].split("---")[0] to CardRepository["rdcard:${
            it[1].replaceAfterLast(
                ".",
                ""
            ).removeSuffix(".")
        }"]
    }
    .toMutableMap()
    .apply {
        put("testforluck", TestForLuck)
        put("bomb", Bomb)
        put("worm", Worm)
    }.toMap()

internal fun transfer() {
    CardUser.getUsers().values.forEach {
        val normalUser = NormalUser[it.id]

        normalUser.clearInventory()

        it.inventory.forEach {
            itemIdMap[it.id]?.let {
                normalUser.giveItem(it) {}
            }
        }

        normalUser += Coin count it.token
    }

    box.clear()
    CardPool.getPools()["box"]!!.cards.forEach {
        itemIdMap[it.id].safeCast<Card>()?.let {
            box.add(it)
        }
    }
}

object DataTransfer : SimpleCommand(
    plugin,
    "transfer",
    description = "数据迁移",
    parentPermission = adminPermission
) {
    @Handler
    fun CommandSender.handle() {
        transfer()
    }
}