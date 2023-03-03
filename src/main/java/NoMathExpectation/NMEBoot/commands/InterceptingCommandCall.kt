@file:OptIn(ConsoleExperimentalApi::class, ExperimentalCommandDescriptors::class)

package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.commandSystem.Alias.aliasIn
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.descriptor.ExperimentalCommandDescriptors
import net.mamoe.mirai.console.command.resolve.CommandCallInterceptor
import net.mamoe.mirai.console.command.resolve.InterceptResult
import net.mamoe.mirai.console.extensions.CommandCallInterceptorProvider
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
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
    }
}