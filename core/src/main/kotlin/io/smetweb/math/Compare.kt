package io.smetweb.math

import java.util.*
import java.util.function.IntSupplier

enum class Compare(val value: Int) {

    /** the value compares as ordinally LESS  */
    LESSER(-1),

    /** the value compares as ordinally EQUIVALENT  */
    EQUIVALENT(0),

    /** the value compares as ordinally GREATER  */
    GREATER(1);

    fun toInt(): Int = this.value

    fun invert(): Compare =
            when (this) {
                LESSER -> GREATER
                GREATER -> LESSER
                else -> this
            }

    companion object {
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> compare(o1: Comparable<T>, o2: Any): Int =
                if (o1 === o2)
                    0
                else
                    o1.compareTo(o2 as T)

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> compare(o1: Any, o2: Any, comparator: Comparator<T>): Int =
                if (o1 === o2)
                    0
                else
                    comparator.compare(o1 as T, o2 as T)

        /**
         * @return `< 0` iff [m2] has less or smaller values than [m1], `0` iff equivalent, `>0` otherwise
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Deprecated("who would use this?")
        fun <K, V: Any> compareMap(m1: SortedMap<K, V>, m2: Map<K, V>): Int {
            if (m1 == m2)
                return 0
            for ((k1, v1) in m1) {
                val v2 = m2[k1]
                        ?: return -1
                if (v1 == null)
                    return 1
                if (v1 != v2) {
                    val comparison: Int = compare(v1 as Comparable<*>, v2)
                    if (comparison != 0)
                        return comparison
                }
            }
            for (k2 in m2.keys)
                if (!m1.containsKey(k2))
                    return 1
            return 0
        }

        @JvmStatic
        fun of(comparison: Int): Compare =
                when (comparison) {
                    0 -> EQUIVALENT
                    else -> if (comparison < 0)
                        LESSER
                    else
                        GREATER
                }

        @JvmStatic
        fun <T> of(o1: Comparable<T>, o2: Any): Compare =
                of(compare(o1, o2))

        @JvmStatic
        fun <T> of(o1: Any, o2: Any, comparator: Comparator<T>): Compare =
                of(compare(o1, o2, comparator))

        /** @return `o1 = o2` */
        @JvmStatic
        fun <T> eq(o1: Comparable<T>, o2: Any): Boolean =
                compare(o1, o2) == 0

        /** @return `o1 < o2`  */
        @JvmStatic
        fun <T> lt(o1: Comparable<T>, o2: Any): Boolean =
                compare(o1, o2) < 0

        /** @return `o1 =< o2` */
        @JvmStatic
        fun <T> le(o1: Comparable<T>, o2: Any): Boolean =
                compare(o1, o2) < 1

        /** @return `o1 > o2` */
        @JvmStatic
        fun <T> gt(o1: Comparable<T>, o2: Any): Boolean =
                compare(o1, o2) > 0

        /** @return `o1 >= o2` */
        @JvmStatic
        fun <T> ge(o1: Comparable<T>, o2: Any): Boolean =
                compare(o1, o2) > -1

        fun unequalOrElse(comparison: Int,
                          equalResolver: () -> Int): Int {
            return if (comparison != 0) comparison else equalResolver()
        }

        @JvmStatic
        fun <T: Any> `is`(self: Comparable<T>): Matcher<T> =
                Matcher(self)
    }

    data class Matcher<T: Any>(private val value: Comparable<T>) {

        fun eq(other: T): Boolean =
                eq(this.value, other)

        fun lt(other: T): Boolean =
                lt(this.value, other)

        fun le(other: T): Boolean =
                le(this.value, other)

        fun gt(other: T): Boolean =
                gt(this.value, other)

        fun ge(other: T): Boolean =
                ge(this.value, other)

    }
}
