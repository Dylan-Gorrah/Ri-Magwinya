package com.rimagwinya.app.core.money

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Money as a whole number of cents.
 *
 * Never a Double. Decimal arithmetic drifts, and a wallet balance that drifts
 * is a balance nobody can reconcile. R12.50 is the Long 1250.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(cents + other.cents)
    operator fun minus(other: Money) = Money(cents - other.cents)
    operator fun times(count: Int) = Money(cents * count)

    override fun compareTo(other: Money) = cents.compareTo(other.cents)

    val isZero: Boolean get() = cents == 0L
    val isPositive: Boolean get() = cents > 0L

    /** "R12.50", or "-R12.50". Always two decimal places. */
    fun format(): String {
        val sign = if (cents < 0) "-" else ""
        val abs = abs(cents)
        return "%sR%d.%02d".format(sign, abs / 100, abs % 100)
    }

    override fun toString() = format()

    companion object {
        val ZERO = Money(0)

        fun ofCents(cents: Long) = Money(cents)

        fun ofRands(rands: Int) = Money(rands * 100L)

        /**
         * Only for reading the server's numeric(10,2) values. Rounds to the
         * nearest cent rather than truncating, so 12.499999 is not R12.49.
         */
        fun fromDecimal(value: Double) = Money((value * 100.0).roundToLong())
    }
}

fun Iterable<Money>.sum(): Money = Money(sumOf { it.cents })
