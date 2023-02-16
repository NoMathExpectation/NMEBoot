package NoMathExpectation.NMEBoot.inventory

import NoMathExpectation.NMEBoot.inventory.card.Card
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.sendMessage
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.buildMessageChain
import kotlin.random.Random

internal fun registerAllItems() {
    registerItem(Coin)
    registerItem(TestForLuck)
    registerItem(Bomb)
    registerItem(Worm)

    registerItemClass<Card>()
}

@Serializable
@SerialName("coin")
object Coin : Item {
    override val id = "coin"
    override val name = "硬币"
    override val description = "只是一枚硬币，也许会有用处呢"

    override val negativeCountable = true
    override val reusable = true

    override suspend fun NormalUser.onUse(): Boolean {
        sendMessage("你抛出一枚硬币，硬币${if (Random.nextBoolean()) '正' else '反'}面朝上")
        return true
    }
}

@Serializable
@SerialName("test_for_luck")
object TestForLuck : Item {
    override val id = "test_for_luck"
    override val name = "运气检测器"
    override val description = "使用此物品，将有50%的几率获得1枚硬币，也将有50%的几率失去1枚硬币"

    override suspend fun NormalUser.onUse(): Boolean {
        if (Random.nextBoolean()) {
            this += Coin
            sendMessage("你获得了1枚硬币")
        } else {
            this -= Coin
            sendMessage("你失去了1枚硬币")
        }
        return true
    }
}

@Serializable
@SerialName("bomb")
object Bomb : Item {
    override val id = "bomb"
    override val name = "雷管"
    override val description = "将此物品扔出去，捡到的人会原地爆炸，扣除1枚硬币"

    override fun NormalUser.onGive() {
        plugin.launch { useItem(this@Bomb) }
    }

    override suspend fun NormalUser.onUse(): Boolean {
        this -= Coin
        sendMessage(buildMessageChain {
            +At(id)
            +" 原地爆炸了！失去了1枚硬币。"
        })
        return true
    }
}

@Serializable
@SerialName("worm")
object Worm : Item {
    override val id = "worm"
    override val name = "蛀虫"
    override val description = "将它扔出去，它会啃掉捡到的人的一个随机物品（不包括硬币）"

    private fun predicate(item: Item): Boolean {
        return item != Worm && !item.negativeCountable
    }

    override fun NormalUser.onGive() {
        plugin.launch { useItem(this@Worm) }
    }

    override suspend fun NormalUser.onUse() = true.also {
        val item = searchItem(this@Worm::predicate).randomOrNull()?.item ?: run {
            sendMessage(buildMessageChain {
                +At(id)
                +" 什么东西也没有，蛀虫不情愿地溜走了。"
            })
            return@also
        }
        -item
        sendMessage(buildMessageChain {
            +"蛀虫啃掉了 "
            +At(id)
            +" 的一个 "
            add(item)
            +" ，味道好极了。"
        })
        return true
    }
}