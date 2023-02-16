package NoMathExpectation.NMEBoot.utils

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission

internal val usePermissionId = plugin.permissionId("use")
internal lateinit var usePermission: Permission private set

internal val adminPermissionId = plugin.permissionId("admin")
internal lateinit var adminPermission: Permission private set

internal fun registerPermissions() {
    usePermission = PermissionService.INSTANCE.register(
        usePermissionId,
        "NMEBoot 使用权限",
        plugin.parentPermission
    )

    adminPermission = PermissionService.INSTANCE.register(
        adminPermissionId,
        "NMEBoot 管理员权限",
        usePermission
    )
}

internal fun CommandSender.hasUsePermission() = hasPermission(usePermissionId)

internal fun CommandSender.hasAdminPermission() = hasPermission(adminPermissionId)