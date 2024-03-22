package NoMathExpectation.NMEBoot.utils

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import kotlinx.coroutines.runBlocking

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@DslMarker
annotation class BrigadierDsl

inline fun <S> CommandDispatcher(block: @BrigadierDsl CommandDispatcher<S>.() -> Unit = {}) =
    CommandDispatcher<S>().apply(block)

inline fun <S> literal(
    name: String,
    block: @BrigadierDsl LiteralArgumentBuilder<S>.() -> Unit = {}
): LiteralArgumentBuilder<S> =
    LiteralArgumentBuilder.literal<S>(name).apply(block)

inline fun <S, AT> requiredArgument(
    name: String,
    type: ArgumentType<AT>,
    block: @BrigadierDsl RequiredArgumentBuilder<S, AT>.() -> Unit = {}
): RequiredArgumentBuilder<S, AT> =
    RequiredArgumentBuilder.argument<S, AT>(name, type).apply(block)

inline fun <S, T1 : ArgumentBuilder<S, T1>, T2 : ArgumentBuilder<S, T2>> @BrigadierDsl ArgumentBuilder<S, T1>.argument(
    argument: ArgumentBuilder<S, T2>,
    block: @BrigadierDsl ArgumentBuilder<S, T2>.() -> Unit = {}
): CommandNode<S> =
    argument.apply(block).build().also { then(it) }

inline fun <S, T : ArgumentBuilder<S, T>, AT> @BrigadierDsl ArgumentBuilder<S, T>.argument(
    name: String,
    type: ArgumentType<AT>,
    block: @BrigadierDsl RequiredArgumentBuilder<S, AT>.() -> Unit = {}
): ArgumentCommandNode<S, AT> =
    requiredArgument(name, type, block).build().also { then(it) }

inline fun <S> @BrigadierDsl CommandDispatcher<S>.register(
    name: String,
    block: @BrigadierDsl LiteralArgumentBuilder<S>.() -> Unit = {}
): LiteralCommandNode<S> =
    register(literal(name, block))

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.literalArgument(
    name: String,
    block: @BrigadierDsl LiteralArgumentBuilder<S>.() -> Unit = {}
): LiteralCommandNode<S> =
    literal(name, block).build().also { then(it) }

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.boolArgument(
    name: String,
    block: @BrigadierDsl RequiredArgumentBuilder<S, Boolean>.() -> Unit = {}
) =
    argument(name, BoolArgumentType.bool(), block)

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.doubleArgument(
    name: String,
    min: Double = -Double.MAX_VALUE,
    max: Double = Double.MAX_VALUE,
    block: @BrigadierDsl RequiredArgumentBuilder<S, Double>.() -> Unit = {}
) =
    argument(name, DoubleArgumentType.doubleArg(min, max), block)

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.floatArgument(
    name: String,
    min: Float = -Float.MAX_VALUE,
    max: Float = Float.MAX_VALUE,
    block: @BrigadierDsl RequiredArgumentBuilder<S, Float>.() -> Unit = {}
) =
    argument(name, FloatArgumentType.floatArg(min, max), block)

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.intArgument(
    name: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    block: @BrigadierDsl RequiredArgumentBuilder<S, Int>.() -> Unit = {}
) =
    argument(name, IntegerArgumentType.integer(min, max), block)

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.longArgument(
    name: String,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
    block: @BrigadierDsl RequiredArgumentBuilder<S, Long>.() -> Unit = {}
) =
    argument(name, LongArgumentType.longArg(min, max), block)

@BrigadierDsl
object RefinedStringArgumentType : ArgumentType<String> by StringArgumentType.string() {
    override fun parse(reader: StringReader): String = with(reader) {
        if (!canRead()) {
            return ""
        }

        val next = peek()
        if (StringReader.isQuotedStringStart(next)) {
            skip()
            return readStringUntil(next)
        }

        val start = cursor
        while (canRead() && peek() != ' ') {
            skip()
        }

        return string.substring(start, cursor)
    }
}

@BrigadierDsl
enum class StringArgumentCaptureType {
    WORD, QUOTABLE, QUOTABLE_LIMITED, GREEDY
}

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.stringArgument(
    name: String,
    type: StringArgumentCaptureType = StringArgumentCaptureType.QUOTABLE,
    block: @BrigadierDsl RequiredArgumentBuilder<S, String>.() -> Unit = {}
) =
    when (type) {
        StringArgumentCaptureType.WORD -> StringArgumentType.word()
        StringArgumentCaptureType.QUOTABLE -> RefinedStringArgumentType
        StringArgumentCaptureType.QUOTABLE_LIMITED -> StringArgumentType.string()
        StringArgumentCaptureType.GREEDY -> StringArgumentType.greedyString()
    }.let { argument(name, it, block) }

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.intHandle(crossinline block: @BrigadierDsl suspend CommandContext<S>.() -> Int) =
    executes { runBlocking { it.block() } }!!

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.boolHandle(crossinline block: @BrigadierDsl suspend CommandContext<S>.() -> Boolean) =
    executes { if (runBlocking { it.block() }) 1 else 0 }!!

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.handle(crossinline block: @BrigadierDsl suspend CommandContext<S>.() -> Unit) =
    executes { runBlocking { it.block() }; 1 }!!

inline fun <reified AT> @BrigadierDsl CommandContext<*>.get(name: String): AT = getArgument(name, AT::class.java)

inline fun <reified AT> @BrigadierDsl CommandContext<*>.getOrNull(name: String): AT? =
    kotlin.runCatching { getArgument(name, AT::class.java) }.getOrNull()

inline fun <reified AT> @BrigadierDsl CommandContext<*>.getOrDefault(name: String, default: AT): AT =
    kotlin.runCatching { getArgument(name, AT::class.java) }.getOrDefault(default)

inline fun <reified AT> @BrigadierDsl CommandContext<*>.getOrElse(name: String, block: (Throwable) -> AT): AT =
    kotlin.runCatching { getArgument(name, AT::class.java) }.getOrElse(block)

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.filter(crossinline predicate: suspend S.() -> Boolean) =
    requires { runBlocking { predicate(it) } }!!

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.targets(
    target: CommandNode<S>,
    fork: Boolean = true,
    crossinline modifier: @BrigadierDsl suspend CommandContext<S>.() -> List<S> = { listOf(source) }
) =
    forward(target, { runBlocking { modifier(it) } }, fork)!!

inline fun <S, T : ArgumentBuilder<S, T>> @BrigadierDsl ArgumentBuilder<S, T>.targetsSingle(
    target: CommandNode<S>,
    fork: Boolean = false,
    crossinline modifier: @BrigadierDsl suspend CommandContext<S>.() -> S = { source }
) =
    targets(target, fork) { listOf(modifier()) }