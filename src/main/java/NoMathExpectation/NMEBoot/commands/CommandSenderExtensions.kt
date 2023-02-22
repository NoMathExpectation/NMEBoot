package NoMathExpectation.NMEBoot.sending

import NoMathExpectation.NMEBoot.inventory.NormalUser
import NoMathExpectation.NMEBoot.inventory.toNormalUser
import NoMathExpectation.NMEBoot.utils.hasAdminPermission
import net.mamoe.mirai.console.command.CommandSender

suspend fun CommandSender.checkAndGetNormalUser(overridden: NormalUser? = null): NormalUser? {
    return if (overridden != null) {
        if (hasAdminPermission()) {
            overridden
        } else {
            sendMessage("你没有权限操作他人的物品栏。")
            null
        }
    } else {
        user?.toNormalUser() ?: run {
            sendMessage("请提供要查询的对象。")
            null
        }
    }
}