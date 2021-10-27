package io.smetweb.random

import io.smetweb.math.DEFAULT_CONTEXT
import java.math.BigDecimal
import java.math.MathContext
import java.util.SortedMap

/**
 * [PseudoRandom] generates a stream of pseudo-random numbers, with API like [java.util.Random] and [kotlin.random.Random]
 * <p>
 * TODO: Implement a thread-safe/multi-threaded default, e.g. <a href=
 * "https://gist.github.com/dhadka/f5a3adc36894cc6aebcaf3dc1bbcef9f">ThreadLocal</a>
 * or
 * <a href="https://github.com/jopasserat/tasklocalrandom">TaskLocalRandom</a>
 */
interface PseudoRandom {

	val seed: Number
		// get() = System.currentTimeMillis() xor System.nanoTime()

	fun nextBoolean(): Boolean

	/** @see fillBytes */
	fun nextBytes(bytes: ByteArray): ByteArray =
		this::nextInt.fillBytes(bytes)

	fun nextInt(): Int

	/** @return an [Int] with `0 =< x =<` bound */
	fun nextIntBelow(boundIncl: Int): Int =
		this::nextInt.coerceBelow(boundIncl)

	fun nextLong(): Long

	fun nextFloat(): Float

	fun nextFloatRange(includeZero: Boolean, includeOne: Boolean): Float {
		var d: Float
		do {
			d = nextFloat() // grab a value, initially from half-open [0.0f, 1.0f)
			if (includeOne && nextBoolean()) d += 1.0f // if includeOne, with 1/2 probability, push to [1.0f, 2.0f)
		} while (d > 1.0f ||  // everything above 1.0f is always invalid
				!includeZero && d == 0.0f) // if we're not including zero, 0.0f is invalid
		return d
	}

	/** @return next [Double] (i.e. 64-bit precision) decimal value in `[0,1]` */
	fun nextDouble(): Double

	fun nextDoubleRange(includeZero: Boolean, includeOne: Boolean): Double {
		var d: Double
		do {
			d = nextDouble() // grab a value, initially from half-open [0.0, 1.0)
			if (includeOne && nextBoolean()) d += 1.0 // if includeOne, with 1/2 probability, push to [1.0, 2.0)
		} while (d > 1.0 ||  // everything above 1.0 is always invalid
				!includeZero && d == 0.0) // if we're not including zero, 0.0 is invalid
		return d
	}

	fun nextGaussian(): Double =
		this::nextDouble.nextGaussian()

	/** @return next 64-bit/double precision [BigDecimal] in `[0,1]` */
	fun nextDecimal(mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
		nextDouble().toBigDecimal(mathContext)

	/**
	 * @param boundIncl > 0 (inclusive)
	 * @return next [Long] with `0 =< x <= bound`
	 */
	fun nextLongBelow(boundIncl: Long): Long =
		this::nextLong.coerceBelow(boundIncl)

	/**
	 * 0 =< min =< max =< (n - 1)
	 *
	 * @param elements non-empty ordered set
	 * @param min lower index bound (inclusive) 0 =< min =< max
	 * @param max upper index bound (exclusive) max > 0
	 * @return element at the next index drawn with uniform probability
	 * @see nextInt
	 */
	fun <E> nextElement(elements: List<E>, min: Int = 0, max: Int = elements.size): E {
		require(elements.isNotEmpty()) { "no elements to sample" }
		require(min >= 0) { "min ($min) < 0" }
		require(max > min) { "min ($min) >= max ($max)" }
		require(min < elements.size) { "min ($min) >= size (${elements.size})" }
		require(max <= elements.size) { "max ($max) > size (${elements.size})" }

		return if (elements.size == 1)
			elements[0]
		else if (min == 0)
			elements[nextIntBelow(max)]
		else
			elements[min + nextIntBelow(max - min)]
	}

	/**
	 * **NOTE** if the elements are not ordered, e.g. a [HashSet], then
	 * results are not guaranteed to be reproducible
	 *
	 * @param elements the [Collection]
	 * @return element at the next index drawn with uniform probability
	 */
	fun <E> nextElement(elements: Collection<E>, max: Int = elements.size): E =
			nextElement(elements, min = 0, max = max)

	fun <E> nextElement(elements: Collection<E>, min: Int = 0, max: Int = elements.size): E {
		require(elements.isNotEmpty()) { "empty" }
		require(min < elements.size) { "min >= size" }
		require(max < elements.size) { "max >= size" }

		return nextElement(elements, min.toLong(), max.toLong())
	}

	/**
	 * **NOTE** if the elements are not ordered, e.g. a [HashSet], then
	 * results are not guaranteed to be reproducible
	 *
	 * @param elements the [Iterable]
	 * @param max bound > 0 (exclusive)
	 * @return the next random element
	 * @see nextInt
	 */
	fun <E> nextElement(elements: Iterable<E>, max: Long): E =
			nextElement(elements, min = 0, max = max)

	fun <E> nextElement(elements: Iterable<E>, min: Long = 0, max: Long): E {
		if (elements is List<*>) {
			return nextElement(elements as List<E>, 0, max.toInt())
		}
		require(min >= 0) { "min < 0" }
		require(max >= min) { "max = min" }
		val n = nextLongBelow(max)
		var i: Long = min
		return elements.first { i++ == n }
	}

	/**
	 * @param map the [SortedMap]
	 * @return the next random element
	 * @see nextIntBelow
	 */
	fun <K, V> nextEntry(map: SortedMap<K, V>): Map.Entry<K, V> {
		val n = nextIntBelow(map.size)
		var i = 0
		return map.entries.first { i++ == n }
	}

	/**
	 * **NOTE** if the elements are not ordered, e.g. a [HashMap], then
	 * results are not guaranteed to be reproducible
	 *
	 * @param map the [Map]
	 * @return the next random element
	 * @see nextIntBelow
	 */
	fun <K, V> nextEntry(map: Map<K, V>): Map.Entry<K, V> {
		val n = nextIntBelow(map.size)
		var i = 0
		return map.entries.first { i++ == n }
	}
}