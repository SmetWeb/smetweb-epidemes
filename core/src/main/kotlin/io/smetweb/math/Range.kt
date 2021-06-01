package io.smetweb.math

import java.util.*
import javax.measure.Quantity
import javax.measure.Unit
import kotlin.Comparator

data class Range<T: Comparable<*>>(
        val lower: Extreme<T> = Extreme.negativeInfinity(),
        val upper: Extreme<T> = Extreme.positiveInfinity()
): Comparable<Range<T>> {

    /**
     * @param value the value (inclusive) or `null` for infinite range
     * @return the [Range] instance
     */
    constructor(value: T? = null):
            this(value, value)

    constructor(minimum: T? = null, maximum: T? = null):
            this(minimum, minimum != null, maximum, false)

    constructor(minimum: T?, minimumInclusive: Boolean, maximum: T?, maximumInclusive: Boolean):
            this(lower = Extreme.lower(minimum, minimumInclusive),
                    upper = Extreme.upper(maximum, maximumInclusive))

	fun lowerValue(): T = lower.value ?: error("Infinite")

	fun lowerInclusive(): Boolean = lower.inclusive ?: error("Infinite")

    fun lowerFinite(): Boolean = lower.isFinite()

    fun upperValue(): T = upper.value ?: error("Infinite")

    fun upperInclusive(): Boolean = upper.inclusive ?: error("Infinite")

    fun upperFinite(): Boolean = upper.isFinite()

	/**
	 * @return `true` iff this [Range] has a finite minimum that is
	 *         greater than specified [value], `false` otherwise
	 */
	fun gt(value: T): Boolean =
            lowerFinite() && if (lowerInclusive())
                Compare.lt(value, lowerValue())
            else
                Compare.le(value, lowerValue())

    /**
     * @param value the [T] to test
     * @return `true` iff this [Range] has a finite maximum that is
     * smaller than specified value, `false` otherwise
     */
    fun lt(value: T): Boolean =
            upperFinite() && if (upperInclusive())
                Compare.gt(value, upperValue())
            else
                Compare.ge(value, upperValue())

    /**
     * @param value the [T] to test
     * @return `true` iff this [Range] contains specified value
     * (i.e. is greater nor lesser)
     */
    fun contains(value: T): Boolean? {
        return !gt(value) && !lt(value)
    }

    fun overlaps(that: Range<T>): Boolean {
        if(this == that)
            return true
        if (that.upperFinite() && gt(that.upperValue()))
            return false
        if (that.lowerFinite() && lt(that.lowerValue()))
            return false
        if (this.upperFinite() && that.gt(upperValue()))
            return false
        return !(this.lowerFinite() && that.lt(lowerValue()))
    }

    fun intersect(that: Range<T>): Range<T>? {
        return if (overlaps(that))
            Range(lower = maxOf(this.lower, that.lower), upper = minOf(this.upper, that.upper))
        else
            null
    }

    fun <R: Comparable<R>> map(mapper: (T?) -> R?): Range<R>? {
        val lower = mapper(lowerValue()) ?: error("Infinite")
        val upper = mapper(upperValue()) ?: error("Infinite")
        return if (lowerFinite() && upperFinite() && Compare.gt(lower, upper))
            Range(upper, upperInclusive(), lower, lowerInclusive()) // reverse
        else
            Range(lower, lowerInclusive(), upper, upperInclusive())
    }

    override fun toString(): String {
        val sb: StringBuilder = StringBuilder()
                .append(if (lowerInclusive()) '[' else '<').append(this.lower)
        if (!lowerFinite() || !upperFinite()
                || lowerValue() != upperValue()) sb.append("; ").append(this.upper)
        sb.append(if (upperInclusive()) ']' else '>')
        return sb.toString()
    }

    override fun compareTo(other: Range<T>): Int {
        return compare(this, other)
    }

    /**
     * @param map the source mapping
     * @return a submap view containing values with intersecting keys
     */
    fun <V> applyTo(map: SortedMap<T, V>): SortedMap<T, V> {
        return map.subMap(lowerValue(), upperValue())
    }

    /**
     * @param map the source [NavigableMap]
     * @param floorLower include the lower bound by flooring it (if possible)
     * @return a [NavigableMap.subMap] view containing only values of intersecting keys
     */
    fun <V> applyTo(map: NavigableMap<T, V>, floorLower: Boolean = false): NavigableMap<T, V> {
        if (map.isEmpty())
            return map
        // use previous key if floorLower==true (given finite lower bound)
        val floor = if (floorLower && lowerFinite()) map.floorKey(lowerValue()) else null
        val from = floor ?: if (lowerFinite()) lowerValue() else map.firstKey()
        val fromIncl = lowerInclusive() || !lowerFinite()
        val to = if (upperFinite()) upperValue() else map.lastKey()
        val toIncl = upperInclusive() || !upperFinite()
        return map.subMap(from, fromIncl, to, toIncl)
    }

    /**
     * @param set the source [NavigableSet]
     * @param floorLower include the lower bound by flooring it (if possible)
     * @return a [NavigableSet.subSet] view containing values of intersecting keys
     */
    fun applyTo(set: NavigableSet<T>,
              floorLower: Boolean): NavigableSet<T>? {
        if (set.isEmpty())
            return set
        // use previous key if floorLower==true (given finite lower bound)
        val floor = if (floorLower && lowerFinite()) set.floor(lowerValue()) else null
        val from = floor ?: if (lowerFinite()) lowerValue() else set.first()
        val fromIncl = lowerInclusive() || !lowerFinite()
        val to = if (upperFinite()) upperValue() else set.last()
        val toIncl = upperInclusive() || !upperFinite()
        return set.subSet(from, fromIncl, to, toIncl)
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun <Q: Quantity<Q>> of(minimum: Number? = null, maximum: Number? = null, unit: Unit<Q> = PURE as Unit<Q>) =
                Range(lower = Extreme.lower(minimum?.toQuantity(unit)), upper = Extreme.upper(maximum?.toQuantity(unit)))

        /** @return a [Range] representing [x,&rarr;) or (x,&rarr;) */
        fun <T: Comparable<*>> upFrom(minimum: T, minimumInclusive: Boolean = false) =
                Range(lower = Extreme.lower(minimum, minimumInclusive), upper = Extreme.positiveInfinity())

        /** @return a [Range] representing (&larr;,x) or (&larr;,x] */
        fun <T: Comparable<*>> upTo(maximum: T, maximumInclusive: Boolean = false) =
                Range(lower = Extreme.negativeInfinity(), upper = Extreme.lower(maximum, maximumInclusive))

        /**
         * @param lhs comparison's left-hand-side
         * @param rhs comparison's right-hand-side
         * @param comparator the comparison's evaluator
         * @return -1, 0 or 1 if lhs is respectively smaller, comparable, or bigger than/to rhs
         */
        fun <T: Comparable<*>> compare(
                lhs: Range<T>,
                rhs: Range<T>,
                comparator: Comparator<in Extreme<T>> = Comparator { o1, o2 -> Compare.compare(o1, o2) }
        ): Int {
            return if (lhs.overlaps(rhs))
                0 // any overlap compares equal (to ensure symmetry)
            else if (lhs.lowerFinite())
                if (rhs.lowerFinite())
                    Compare.unequalOrElse(comparator.compare(lhs.lower, rhs.lower)) {
                        if (lhs.upperFinite())
                            if (rhs.upperFinite())
                                comparator.compare(lhs.upper, rhs.upper) // ;fin ? ;fin
                            else
                                -1 // ;fin < ;+inf
                        else if (rhs.upperFinite())
                            1 // ;+inf > ;fin
                        else
                            0 // ;+inf = ;+inf
                    }
                else
                    1 // fin;* > -inf;*
            else if (rhs.lowerFinite())
                -1 // -inf;* < fin;*
            else if (lhs.upperFinite()) // -inf;* = -inf;*
                if (rhs.upperFinite())
                    comparator.compare(lhs.upper, rhs.upper) // -inf;fin ? -inf;fin
                else
                    1 // -inf;fin > -inf;+inf
            else if (rhs.upperFinite())
                -1 // -inf;+inf < -inf;fin
            else
                0 // -inf;+inf = -inf;+inf
        }
    }
}
