package NoMathExpectation.NMEBoot.utils

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission

internal val adminPermission = plugin.permissionId("admin")

internal fun registerAdminPermission() = PermissionService.INSTANCE.register(
    adminPermission,
    "NMEBoot 管理员权限",
    plugin.parentPermission
)

internal fun CommandSender.hasAdminPermission() = hasPermission(adminPermission)