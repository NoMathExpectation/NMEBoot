package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.inventory.*
import NoMathExpectation.NMEBoot.inventory.Pool.Companion.box
import NoMathExpectation.NMEBoot.inventory.Pool.Companion.ground
import NoMathExpectation.NMEBoot.inventory.card.Card
import NoMathExpectation.NMEBoot.sending.asCustom
import NoMathExpectation.NMEBoot.sending.checkAndGetNormalUser
import NoMathExpectation.NMEBoot.utils.hasAdminPermission
import NoMathExpectation.NMEBoot.utils.logger
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.usePermission
import net.mamoe.mirai.console.command.AbstractUserCommandSender
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.descriptor.buildCommandArgumentContext
import net.mamoe.mirai.console.util.safeCast
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.buildMessageChain

internal object CommandCard : CompositeCommand(
    plugin,
    "card",
    "c",
    description = "card help",
    parentPermission = usePermission,
    overrideContext = buildCommandArgumentContext {
        NormalUser::class with NormalUserCommandParser
        Item::class with ItemCommandParser
    }
) {
    @SubCommand("inventory", "inv")
    @Description("查看物品栏")
    suspend fun CommandSender.getInventory(normalUser: NormalUser? = null) = with(asCustom()) {
        val finalNormalUser = checkAndGetNormalUser(normalUser) ?: return@with

        sendFoldMessage(buildMessageChain {
            +At(finalNormalUser.id)
            +" 的物品有：\n"

            finalNormalUser.getInventory().forEach {
                add(it)
                +"\n"
            }
        })
    }

    @SubCommand("show")
    @Description("搜索并查看物品")
    suspend fun CommandSender.showItem(raw: String, normalUser: NormalUser? = null) = with(asCustom()) {
        val finalNormalUser = checkAndGetNormalUser(normalUser) ?: return@with
        val itemStacks = finalNormalUser.searchItem(raw)
        when (itemStacks.size) {
            0 -> sendMessage("未找到物品。")
            1 -> sendFoldMessage(itemStacks.first().item.show())
            else -> sendFoldMessage("找到${itemStacks.size}个物品：\n${itemStacks.joinToString("\n") { it.showSimple(true) }}")
        }
    }

    @SubCommand("give")
    @Description("给予物品")
    suspend fun CommandSender.giveItemStack(normalUser: NormalUser, item: Item, amount: Int = 1) = with(asCustom()) {
        if (!hasAdminPermission()) {
            sendMessage("你没有权限使用此指令。")
            return@with
        }


        val itemStack = item count amount
        normalUser.giveItemStack(itemStack) {}
        sendMessage(buildMessageChain {
            +"已给予 "
            +At(normalUser.id)
            +" "
            add(itemStack)
        })
    }

    @SubCommand("use")
    @Description("使用物品")
    suspend fun AbstractUserCommandSender.useItem(raw: String) = with(asCustom()) {
        val normalUser = toNormalUser() ?: return@with

        val itemStacks = normalUser.searchItem { it.id.contains(raw) }
        if (itemStacks.isEmpty()) {
            sendMessage("未找到物品。")
            return@with
        }
        if (itemStacks.size > 1) {
            sendFoldMessage(
                "找到${itemStacks.size}个物品，请明确你所要选择的物品：\n${
                    itemStacks.joinToString("\n") {
                        it.showSimple(
                            true
                        )
                    }
                }"
            )
            return@with
        }

        val item = itemStacks.first().item
        kotlin.runCatching { normalUser.useItem(item) }.fold(
            onSuccess = {
                if (!it) {
                    sendMessage("无法使用 ${item.showSimple()}。")
                }
            },
            onFailure = {
                logger.error(it)
                it.message?.let { exceptionMessage -> sendMessage(exceptionMessage) }
            }
        )
    }

    @SubCommand("throw")
    @Description("扔出物品")
    suspend fun AbstractUserCommandSender.throwItem(raw: String) = with(asCustom()) {
        val normalUser = toNormalUser() ?: return@with

        val itemStacks = normalUser.searchItem(raw)
        val itemStack = itemStacks.randomOrNull()?.count(1) ?: run {
            sendMessage("未找到物品。")
            return@with
        }

        normalUser -= itemStack
        ground += itemStack.item
        sendMessage(buildMessageChain {
            +At(normalUser.id)
            +" 扔出了一个物品！"
        })
    }

    @SubCommand("catch")
    @Description("捡起物品")
    suspend fun AbstractUserCommandSender.catchItem() = with(asCustom()) {
        val normalUser = toNormalUser() ?: return

        val item = ground.pull() ?: run {
            sendMessage("地板上空空如也......")
            return
        }

        sendMessage(buildMessageChain {
            +At(normalUser.id)
            +" 捡到了一个 ${item.showSimple()} ！"
        })
        normalUser += item
    }

    enum class BoxOperation(val cost: Int) { IN(-1), OUT(2), BOTH(0) }

    @SubCommand("box", "b")
    @Description("从盒子里取出或放入卡片，放入会获得一枚硬币，取出需消耗两枚硬币，同时执行不耗费硬币")
    suspend fun AbstractUserCommandSender.box(op: BoxOperation = BoxOperation.BOTH) = with(asCustom()) {
        val normalUser = toNormalUser() ?: return

        if (!normalUser.tryAndDiscardItemStack(Coin count op.cost)) {
            sendMessage("你没有足够的硬币。")
            return
        }

        if (op == BoxOperation.IN || op == BoxOperation.BOTH) {
            val card = normalUser.searchItem { it is Card }.randomOrNull().safeCast<Card>() ?: run {
                sendMessage("你没有任何卡片。")
                normalUser += Coin count op.cost
                return
            }

            normalUser -= card
            box += card
        }

        if (op == BoxOperation.OUT || op == BoxOperation.BOTH) {
            val card = box.pull() ?: run {
                sendMessage("盒子里空空如也......")
                normalUser += Coin count op.cost
                return
            }

            normalUser += card
        }
    }
}