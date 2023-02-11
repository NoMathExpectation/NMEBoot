package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.inventory.*
import NoMathExpectation.NMEBoot.utils.hasAdminPermission
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.sendFoldMessage
import net.mamoe.mirai.console.command.AbstractUserCommandSender
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.descriptor.buildCommandArgumentContext
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.buildMessageChain

object CommandCard : CompositeCommand(
    plugin,
    "card",
    "c",
    description = "card help",
    overrideContext = buildCommandArgumentContext {
        NormalUser::class with NormalUserCommandParser
        Item::class with ItemCommandParser
    }
) {
    init {

    }

    @SubCommand("inventory", "inv")
    suspend fun CommandSender.getInventory(normalUser: NormalUser? = null) {
        val id = if (normalUser != null) {
            if (hasAdminPermission()) {
                normalUser.id
            } else {
                sendMessage("你没有权限查看他人的物品栏。")
                return
            }
        } else {
            user?.id ?: run {
                sendMessage("请提供要查询的对象。")
                return
            }
        }

        sendFoldMessage(buildMessageChain {
            +At(id)
            +" 的物品有：\n"

            NormalUser[id].getInventory().forEach {
                add(it)
                +"\n"
            }
        })
    }

    @SubCommand("give")
    suspend fun CommandSender.giveItemStack(normalUser: NormalUser, item: Item, amount: Int = 1) {
        if (!hasAdminPermission()) {
            sendMessage("你没有权限使用此指令。")
            return
        }


        val itemStack = item count amount
        normalUser.giveItemStack(itemStack)
        sendMessage(buildMessageChain {
            +"已给予 "
            +At(normalUser.id)
            +" "
            add(itemStack)
        })
    }

    @SubCommand("use")
    suspend fun AbstractUserCommandSender.useItem(id: String) {
        val normalUser = NormalUser[user.id]

        val itemStacks = normalUser.searchItem { it.id.contains(id) }
        if (itemStacks.isEmpty()) {
            sendMessage("未找到物品。")
            return
        }
        if (itemStacks.size > 1) {
            sendFoldMessage("找到多个物品，请明确你所要选择的物品：\n${itemStacks.joinToString("\n") { it.showSimple(true) }}")
            return
        }

        val item = itemStacks.first().item
        kotlin.runCatching { normalUser.useItem(item) }.fold(
            onSuccess = {
                if (!it) {
                    sendMessage("无法使用 ${item.showSimple()}。")
                }
            },
            onFailure = {
                it.message?.let { exceptionMessage -> sendMessage(exceptionMessage) }
            }
        )
    }
}