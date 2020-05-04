package io.smetweb.math

data class Extreme<T: Comparable<*>>(
        private val boundary: Boundary,
        val value: T? = null,
        val inclusive: Boolean? = value != null
): Comparable<Extreme<T>> {

    constructor(boundary: Boundary, value: T? = null, inclusive: Boolean = false): this(
            boundary = boundary,
            value = value,
            inclusive = value?.let { inclusive })

    /** [Boundary] refers to an extreme of some linear [Range] */
    enum class Boundary(
            /** the infinity comparison value  */
            val limitCompare: Compare
    ) {
        /** also has lower [ordinal] for natural ordering  */
        LOWER(Compare.GREATER),

        /** also has higher [ordinal] for natural ordering  */
        UPPER(Compare.LESSER);
    }

    /** @return `true` iff this value not represents INFINITY */
    fun isFinite(): Boolean =
            this.value != null

    /** @return `true` iff this value represents INFINITY */
    fun isInfinite(): Boolean =
            this.value == null

    /** @return `true` iff this value represents POSITIVE INFINITY */
    fun isPositiveInfinity(): Boolean =
            isInfinite() && isUpper()

    /** @return `true` iff this value represents NEGATIVE INFINITY */
    fun isNegativeInfinity(): Boolean =
            isInfinite() && isLower()

    fun isUpper(): Boolean =
            this.boundary == Boundary.UPPER

    fun isLower(): Boolean =
            this.boundary == Boundary.LOWER

    fun <R: Comparable<*>> map(mapper: (T) -> R): Extreme<R> =
            Extreme<R>(boundary = this.boundary,
                    value = this.value?.let { mapper(it) },
                    inclusive = this.inclusive)

    override fun toString(): String {
        return if (isPositiveInfinity())
            "+inf"
        else if (isNegativeInfinity())
            "-inf"
        else
            this.value.toString()
    }

    override fun compareTo(other: Extreme<T>): Int =
            compareWith(other).toInt()

    private fun compareWith(that: Extreme<T>): Compare {
        if (isInfinite())
            return if (that.isInfinite())
                Compare.of(this.boundary, that.boundary)
            else
                that.boundary.limitCompare
        if (that.isInfinite())
            return that.boundary.limitCompare
        val valueCmp: Compare = Compare.of(this.value!!, that.value!!)
        if (valueCmp !== Compare.EQUIVALENT)
            return valueCmp

        // equivalent values, check inclusiveness
        if (this.inclusive!! && !that.inclusive!!)
            return this.boundary.limitCompare
        return if (!this.inclusive && that.inclusive!!)
            that.boundary.limitCompare
        else
            Compare.EQUIVALENT
    }

    companion object {

        private val NEGATIVE_INFINITY: Extreme<*> = Extreme(Boundary.LOWER, null, false)

        private val POSITIVE_INFINITY: Extreme<*> = Extreme(Boundary.UPPER, null, false)

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T: Comparable<*>> negativeInfinity(): Extreme<T> =
                NEGATIVE_INFINITY as Extreme<T>

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T: Comparable<*>> positiveInfinity(): Extreme<T> =
                POSITIVE_INFINITY as Extreme<T>

        @JvmStatic
        fun <T: Comparable<*>> lower(minimum: T? = null, inclusive: Boolean = false): Extreme<T> =
                minimum?.let { Extreme(Boundary.LOWER, it, inclusive) } ?: negativeInfinity()

        @JvmStatic
        fun <T: Comparable<*>> upper(maximum: T? = null, inclusive: Boolean = false): Extreme<T> =
                maximum?.let { Extreme(Boundary.UPPER, it, inclusive) } ?: positiveInfinity()
    }
}
