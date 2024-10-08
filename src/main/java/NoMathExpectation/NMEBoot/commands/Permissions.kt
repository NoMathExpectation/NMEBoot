package NoMathExpectation.NMEBoot.utils

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.MemberCommandSender
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.contact.isOperator

internal val usePermissionId = plugin.permissionId("use")
internal lateinit var usePermission: Permission private set

internal val adminPermissionId = plugin.permissionId("admin")
internal lateinit var adminPermission: Permission private set

internal val rdlPermissionId = plugin.permissionId("rdlounge")
internal lateinit var rdlPermission: Permission private set

internal fun registerPermissions() {
    adminPermission = PermissionService.INSTANCE.register(
        adminPermissionId,
        "NMEBoot 管理员权限",
        plugin.parentPermission
    )

    rdlPermission = PermissionService.INSTANCE.register(
        rdlPermissionId,
        "NMEBoot RDLounge群权限",
        adminPermission
    )

    usePermission = PermissionService.INSTANCE.register(
        usePermissionId,
        "NMEBoot 使用权限",
        rdlPermission
    )
}

internal fun CommandSender.hasUsePermission() = hasPermission(usePermissionId)

internal fun CommandSender.hasAdminPermission() = hasPermission(adminPermissionId)

internal fun CommandSender.hasRDLPermission() = hasPermission(rdlPermissionId)

internal fun CommandSender.isGroupAdmin() = this is MemberCommandSender && user.isOperator()