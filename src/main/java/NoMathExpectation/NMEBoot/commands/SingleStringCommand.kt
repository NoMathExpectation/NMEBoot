package NoMathExpectation.NMEBoot.commands

import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandOwner
import net.mamoe.mirai.console.command.RawCommand
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.message.data.MessageChain

abstract class SingleStringCommand(
    override val owner: CommandOwner,
    override val primaryName: String,
    override vararg val secondaryNames: String,
    override val usage: String = "<no usages given>",
    override val description: String = "<no description given>",
    parentPermission: Permission = owner.parentPermission
) : RawCommand(
    owner,
    primaryName,
    *secondaryNames,
    usage = usage,
    description = description,
    parentPermission = parentPermission
) {
    private val regex = sequenceOf(sequenceOf(primaryName), secondaryNames.asSequence())
        .flatMap { it }
        .joinToString("|", "(${CommandManager.commandPrefix})?(", ")?[\\s\\h\\v]*")
        .toRegex()

    override suspend fun CommandContext.onCommand(args: MessageChain) {
        handle(originalMessage.contentToString().replaceFirst(regex, ""))
    }

    abstract suspend fun CommandContext.handle(text: String)
}