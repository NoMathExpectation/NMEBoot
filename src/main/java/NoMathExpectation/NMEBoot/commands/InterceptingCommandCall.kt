@file:OptIn(ConsoleExperimentalApi::class, ExperimentalCommandDescriptors::class)

package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.commandSystem.Alias.aliasIn
import NoMathExpectation.NMEBoot.sending.Cooldown
import NoMathExpectation.NMEBoot.utils.TimeRefreshable
import NoMathExpectation.NMEBoot.utils.hasAdminPermission
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.descriptor.ExperimentalCommandDescriptors
import net.mamoe.mirai.console.command.resolve.CommandCallInterceptor
import net.mamoe.mirai.console.command.resolve.InterceptResult
import net.mamoe.mirai.console.command.resolve.InterceptedReason
import net.mamoe.mirai.console.command.resolve.ResolvedCommandCall
import net.mamoe.mirai.console.extensions.CommandCallInterceptorProvider
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.Message

internal object InterceptingCommandCall : CommandCallInterceptorProvider {
    override val instance = object : CommandCallInterceptor {
        override fun interceptBeforeCall(message: Message, caller: CommandSender): InterceptResult<Message>? {
            val contact = caller.subject ?: return null

            if (message.contentToString().trim()
                    .startsWith("${CommandManager.commandPrefix}${CommandAlias.primaryName}")
            ) {
                return null
            }

            return InterceptResult(message.aliasIn(contact))
        }

        override fun interceptResolvedCall(call: ResolvedCommandCall): InterceptResult<ResolvedCommandCall>? {
            val cmdSender = call.caller
            if (cmdSender.hasAdminPermission()) {
                return null
            }

            val group = cmdSender.subject as? Group ?: return null
            if (Cooldown.consumeCooldown(group.id)) {
                return null
            }

            val counter = Cooldown[group.id]
            val string = if (counter is TimeRefreshable) {
                "请等待${counter.timeUntilRefresh.inWholeSeconds}s后使用指令"
            } else {
                "指令使用次数已用完"
            }

            if (call.callee.permission.testPermission(call.caller)) {
                call.caller.launch { call.caller.sendMessage(string) }
            }

            return InterceptResult(InterceptedReason(string))
        }
    }
}