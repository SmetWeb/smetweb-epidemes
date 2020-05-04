package io.smetweb.random

import java.math.BigDecimal
import java.util.SortedMap

/**
 * [PseudoRandom] generates a stream of pseudo-random numbers
 * <p>
 * TODO: Implement a thread-safe/multi-threaded default, e.g. <a href=
 * "https://gist.github.com/dhadka/f5a3adc36894cc6aebcaf3dc1bbcef9f">ThreadLocal</a>
 * or
 * <a href="https://github.com/jopasserat/tasklocalrandom">TaskLocalRandom</a>
 * @see PseudoRandomJava
 * @see PseudoRandomKotlin
 */
interface PseudoRandom {

	val seed: Number
		get() = System.currentTimeMillis() xor System.nanoTime()

	fun nextBoolean(): Boolean

	/** @see fillBytes */
	fun nextBytes(bytes: ByteArray): ByteArray =
			this::nextInt.fillBytes(bytes)

	fun nextInt(): Int

	/** @see coerceBelow */
	fun nextIntBelow(bound: Int): Int =
			this::nextInt.coerceBelow(bound)

	fun nextLong(): Long

	fun nextFloat(): Float

	/** @return next [Double] (i.e. 64-bit precision) decimal value in [0,1] */
	fun nextDouble(): Double

	fun nextGaussian(): Double =
			this::nextDouble.nextGaussian()

	/** @return next 64-bit/double precision [BigDecimal] in [0,1] */
	fun nextDecimal(): BigDecimal =
			nextDouble().toBigDecimal()

	/**
	 * @param bound > 0 (exclusive)
	 * @return 0 <= x < bound
	 * @see coerceBelow
	 */
	fun nextLongBelow(bound: Long): Long =
			this::nextLong.coerceBelow(bound)

	fun <E> nextElement(elements: List<E>, max: Int = elements.size): E =
			nextElement(elements, 0, max)

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
		require(elements.isNotEmpty()) { "empty" }
		require(min >= 0) { "min < 0" }
		require(min < elements.size) { "min >= size" }
		require(max < elements.size) { "max >= size" }
		require(max >= min) { "max = min" }

		return if (elements.size == 1)
			elements[0]
		else if (max == min)
			elements[min]
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