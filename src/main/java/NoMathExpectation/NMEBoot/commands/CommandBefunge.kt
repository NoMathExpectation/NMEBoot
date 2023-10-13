package NoMathExpectation.NMEBoot.commands

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import NoMathExpectation.NMEBoot.utils.usePermission
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig

object CommandBefunge : SingleStringCommand(
    plugin,
    "befunge",
    "bf93",
    usage = "${CommandManager.commandPrefix}bf93\n<expr>",
    description = "Eval Befunge-93. https://esolangs.org/wiki/Befunge",
    parentPermission = usePermission
) {
    private object Config : AutoSavePluginConfig("befunge") {
        val opsLimit: Long by value(1000_0000L)
        val outputLimit: Int by value(1000)

        init {
            reload {
                plugin.reloadPluginConfig(this)
            }
        }
    }

    private enum class Direction(val dx: Int, val dy: Int) {
        LEFT(-1, 0), RIGHT(1, 0), UP(0, -1), DOWN(0, 1)
    }

    fun eval(memory: MutableMap<Pair<Int, Int>, Char>): String {
        var xBound = 0
        var yBound = 0
        memory.forEach { (k, _) ->
            xBound = maxOf(xBound, k.first)
            yBound = maxOf(yBound, k.second)
        }

        var x = 0
        var y = 0
        var dir = Direction.RIGHT
        fun move() {
            x += dir.dx
            y += dir.dy

            if (x < 0) x = xBound
            if (x > xBound) x = 0
            if (y < 0) y = yBound
            if (y > yBound) y = 0
        }

        fun StringBuilder.exceptionString(x: Int, y: Int, char: Char, reason: String) =
            append("\nException thrown at instruction $char at ($x, $y): $reason")

        fun <T> MutableList<T>.push(element: T) = add(element)
        fun <T> MutableList<T>.pop() = removeAt(lastIndex)

        val stack = mutableListOf<Int>()
        var stringMode = false
        var ops = 0L

        var output = buildString {
            while (true) {
                if (++ops > Config.opsLimit) {
                    append("\nTime limit exceeded.")
                    break
                }

                val op = memory[x to y] ?: ' '
                if (stringMode) {
                    if (op == '"') {
                        stringMode = false
                    } else {
                        stack.push(op.code)
                    }
                } else when (op) {
                    '+' -> {
                        if (stack.size < 2) {
                            exceptionString(x, y, '+', "Not enough elements in stack.")
                            return@buildString
                        }
                        val a = stack.pop()
                        val b = stack.pop()
                        stack.push(a + b)
                    }

                    '-' -> {
                        if (stack.size < 2) {
                            exceptionString(x, y, '-', "Not enough elements in stack.")
                            return@buildString
                        }
                        val a = stack.pop()
                        val b = stack.pop()
                        stack.push(b - a)
                    }

                    '*' -> {
                        if (stack.size < 2) {
                            exceptionString(x, y, '*', "Not enough elements in stack.")
                            return@buildString
                        }
                        val a = stack.pop()
                        val b = stack.pop()
                        stack.push(a * b)
                    }

                    '/' -> {
                        if (stack.size < 2) {
                            exceptionString(x, y, '/', "Not enough elements in stack.")
                            return@buildString
                        }
                        val a = stack.pop()
                        val b = stack.pop()
                        stack.push(if (a == 0) 0 else b / a)
                    }

                    '%' -> {
                        if (stack.size < 2) {
                            exceptionString(x, y, '%', "Not enough elements in stack.")
                            return@buildString
                        }
                        val a = stack.pop()
                        val b = stack.pop()
                        stack.push(if (a == 0) 0 else b % a)
                    }

                    '!' -> {
                        if (stack.isEmpty()) {
                            exceptionString(x, y, '!', "Not enough elements in stack.")
                            return@buildString
                        }
                        val a = stack.pop()
                        stack.push(if (a == 0) 1 else 0)
                    }

                    '`' -> {
                        if (stack.size < 2) {
                            exceptionString(x, y, '`', "Not enough elements in stack.")
                            return@buildString
                        }
                        val a = stack.pop()
                        val b = stack.pop()
                        stack.push(if (b > a) 1 else 0)
                    }

                    '>' -> dir = Direction.RIGHT
                    '<' -> dir = Direction.LEFT
                    '^' -> dir = Direction.UP
                    'v' -> dir = Direction.DOWN
                    '?' -> dir = Direction.values().random()

                    '_' -> {
                        if (stack.isEmpty()) {
                            exceptionString(x, y, '_', "Not enough elements in stack.")
                            return@buildString
                        }
                        dir = if (stack.pop() == 0) Direction.RIGHT else Direction.LEFT
                    }

                    '|' -> {
                        if (stack.isEmpty()) {
                            exceptionString(x, y, '|', "Not enough elements in stack.")
                            return@buildString
                        }
                        dir = if (stack.pop() == 0) Direction.DOWN else Direction.UP
                    }

                    '"' -> stringMode = true

                    ':' -> {
                        if (stack.isEmpty()) {
                            stack.push(0)
                        } else {
                            val a = stack.pop()
                            stack.push(a)
                            stack.push(a)
                        }
                    }

                    '\\' -> {
                        if (stack.isEmpty()) {
                            exceptionString(x, y, '\\', "Not enough elements in stack.")
                            return@buildString
                        }
                        val a = stack.pop()
                        val b = if (stack.isEmpty()) 0 else stack.pop()
                        stack.push(a)
                        stack.push(b)
                    }

                    '$' -> {
                        if (stack.isEmpty()) {
                            exceptionString(x, y, '$', "Not enough elements in stack.")
                            return@buildString
                        }
                        stack.pop()
                    }

                    '.' -> {
                        if (stack.isEmpty()) {
                            exceptionString(x, y, '.', "Not enough elements in stack.")
                            return@buildString
                        }
                        append(stack.pop())
                    }

                    ',' -> {
                        if (stack.isEmpty()) {
                            exceptionString(x, y, ',', "Not enough elements in stack.")
                            return@buildString
                        }
                        append(stack.pop().toChar())
                    }

                    '#' -> move()

                    'g' -> @Suppress("NAME_SHADOWING") {
                        if (stack.size < 2) {
                            exceptionString(x, y, 'g', "Not enough elements in stack.")
                            return@buildString
                        }
                        val y = stack.pop()
                        val x = stack.pop()
                        stack.push(memory[x to y]?.code ?: 0)
                    }

                    'p' -> @Suppress("NAME_SHADOWING") {
                        if (stack.size < 3) {
                            exceptionString(x, y, 'p', "Not enough elements in stack.")
                            return@buildString
                        }
                        val y = stack.pop()
                        val x = stack.pop()
                        val v = stack.pop()
                        memory[x to y] = v.toChar()

                        xBound = maxOf(xBound, x)
                        yBound = maxOf(yBound, y)
                    }

                    '&', '~' -> {
                        exceptionString(x, y, op, "This operation is not supported yet.")
                        return@buildString
                    }

                    '@' -> return@buildString

                    else -> {
                        if (op.isDigit()) {
                            stack.push(op.digitToInt())
                        }
                    }
                }

                move()
            }
        }

        if (output.length > Config.outputLimit) {
            output = output.substring(0 until Config.outputLimit) + "\n Output limit exceeded."
        }
        if (output.isEmpty()) {
            output = "No output."
        }

        return output
    }

    override suspend fun CommandContext.handle(text: String) {
        val memory = mutableMapOf<Pair<Int, Int>, Char>()
        text.split("\n")
            .forEachIndexed { y, line ->
                line.forEachIndexed { x, char ->
                    memory[x to y] = char
                }
            }

        try {
            sender.sendMessage(eval(memory))
        } catch (e: Exception) {
            e.printStackTrace()
            sender.sendMessage(e.message ?: "An error occurred.")
        }
    }
}