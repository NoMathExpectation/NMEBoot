package NoMathExpectation.NMEBoot.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
sealed class UseCounter() {
    abstract val useCount: Int

    var remain = useCount

    open fun use() = canUse().also {
        if (it) remain--
    }

    open fun canUse() = remain > 0

    open fun reset() {
        remain = useCount
    }

    companion object {
        operator fun invoke(useCount: Int) = LimitedTimeUseCounter(useCount)
    }
}

@Serializable
@SerialName("limited-time-use-counter")
class LimitedTimeUseCounter(override val useCount: Int) : UseCounter()

@Serializable
@SerialName("fixed-delay-use-counter")
class FixedDelayUseCounter(
    override val useCount: Int,
    val delay: SerializableDuration,
    var nextRefresh: Instant = Instant.DISTANT_PAST
) : UseCounter() {
    override fun use() = canUse().also {
        if (it) remain--
    }

    override fun canUse(): Boolean {
        val now = Clock.System.now()
        if (now >= nextRefresh) {
            nextRefresh = now + delay
            reset()
        }
        return super.canUse()
    }
}

@Serializable
@SerialName("fixed-rate-use-counter")
class FixedRateUseCounter constructor(
    override val useCount: Int,
    val rate: SerializableDuration,
    var nextRefresh: Instant = Clock.System.now()
) : UseCounter() {
    override fun use() = canUse().also {
        if (it) remain--
    }

    override fun canUse(): Boolean {
        val now = Clock.System.now()
        if (now >= nextRefresh) {
            while (nextRefresh < now) {
                nextRefresh += rate
            }
            reset()
        }
        return super.canUse()
    }

    companion object {
        fun ofDay(useCount: Int): FixedRateUseCounter {
            val now = Clock.System.now().epochSeconds
            return FixedRateUseCounter(useCount, Duration.parse("1d"), Instant.fromEpochSeconds(now - now % 86400L))
        }
    }
}