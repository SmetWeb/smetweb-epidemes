package io.smetweb.math

import java.math.BigDecimal
import java.util.*

/**
 * {@link PseudoRandom} generates a stream of pseudo-random numbers, with an API
 * similar to the standard Java {@link Random} generator (which is wrapped
 * accordingly in the {@link JavaRandom} decorator)
 * <p>
 * <b>TODO</b> Implement a thread-safe/multi-threaded default, e.g. <a href=
 * "https://gist.github.com/dhadka/f5a3adc36894cc6aebcaf3dc1bbcef9f">ThreadLocal</a>
 * or
 * <a href="https://github.com/jopasserat/tasklocalrandom">TaskLocalRandom</a>
 */
interface PseudoRandom {

	fun seed(): Number

	/** @see Random.nextBoolean */
	fun nextBoolean(): Boolean

	/** @see Random.nextBytes */
	fun nextBytes(bytes: ByteArray)

	/** @see Random.nextInt */
	fun nextInt(): Int

	/** @see Random.nextInt */
	fun nextInt(bound: Int): Int

	/** @see Random.nextLong */
	fun nextLong(): Long

	/** @see Random.nextFloat */
	fun nextFloat(): Float

	/**
	 * @return next [Double] (i.e. 64-bit precision) floating-point value
	 *  [0,1]
	 *
	 * @see Random.nextDouble
	 */
	fun nextDouble(): Double

	/** @see Random.nextGaussian */
	fun nextGaussian(): Double = nextGaussian(this)

	/** @return next 64-bit/double precision [BigDecimal] in [0,1] */
	fun nextDecimal(): BigDecimal =
			nextDouble().toBigDecimal()

	/**
	 * @param bound > 0 (exclusive)
	 * @return 0 <= x < bound
	 * @see Random.nextInt
	 */
	fun nextLong(bound: Long): Long {
		if (bound < 0) {
			throw IllegalArgumentException("bound < 0")
		}

		// skip 2^n matching, as per http://stackoverflow.com/a/2546186
		var bits: Long
		var result: Long
		do {
			bits = nextLong() shl 1 ushr 1
			result = bits % bound
		} while (bits - result + (bound - 1) < 0L)
		return result
	}

	/**
	 * @param elements an ordered collection
	 * @return next element drawn with uniform probability
	 */
	fun <E> nextElement(elements: List<E>): E {
		if (elements.isEmpty()) {
			throw IllegalArgumentException("Nothing to pick from")
		}
		return if (elements.size == 1)
			elements[0]
		else
			nextElement(elements, 0, elements.size)
	}

	/**
	 * 0 =< min =< max =< (n - 1)
	 *
	 * @param elements non-empty ordered set
	 * @param min lower index bound (inclusive) 0 =< min =< max
	 * @param max upper index bound (exclusive) max > 0
	 * @return element at the next index drawn with uniform probability
	 * @see Random.nextInt
	 */
	fun <E> nextElement(elements: List<E>, min: Int, max: Int): E {
		// sanity checks
		if (elements.isEmpty()) {
			throw IllegalArgumentException("empty")
		}
		if (min < 0) {
			throw IllegalArgumentException("min < 0")
		}
		if (min >= elements.size) {
			throw ArrayIndexOutOfBoundsException("min >= size")
		}
		if (max < min) {
			throw IllegalArgumentException("max < min")
		}
		if (max > elements.size) {
			throw ArrayIndexOutOfBoundsException("max > size")
		}
		return if (elements.size == 1)
			elements[0]
		else
			elements[min + nextInt(max - min)]
	}

	/**
	 * **NOTE** if the elements are not ordered, e.g. a [HashSet], then
	 * results are not guaranteed to be reproducible
	 *
	 * @param elements the [Collection]
	 * @return element at the next index drawn with uniform probability
	 */
	fun <E> nextElement(elements: Collection<E>): E {
		if (elements is List<*>) {
			return nextElement(elements as List<E>)
		}
		return if (elements.isEmpty())
			throw IllegalArgumentException("empty")
		else
			nextElement(elements, elements.size.toLong())
	}

	/**
	 * **NOTE** if the elements are not ordered, e.g. a [HashSet], then
	 * results are not guaranteed to be reproducible
	 *
	 * @param elements the [Collection]
	 * @param max bound > 0 (exclusive)
	 * @return element at the next index drawn with uniform probability
	 * @see Random.nextInt
	 */
	fun <E> nextElement(elements: Collection<E>, max: Long): E {
		if (elements is List<*>) return nextElement(elements as List<E>, 0, max.toInt())
		return if (elements.isEmpty())
			throw throw IllegalArgumentException("empty")
		else
			nextElement(elements as Iterable<E>, max)
	}

	/**
	 * **NOTE** if the elements are not ordered, e.g. a [HashSet], then
	 * results are not guaranteed to be reproducible
	 *
	 * @param elements the [Iterable]
	 * @param max bound > 0 (exclusive)
	 * @return the next random element
	 * @see Random.nextInt
	 */
	fun <E> nextElement(elements: Iterable<E>, max: Long): E {
		if (elements is List<*>) {
			return nextElement(elements as List<E>, 0, max.toInt())
		}
		val it = Objects.requireNonNull(elements).iterator()
		val n = nextLong(max)
		var i: Long = 0
		while (it.hasNext()) {
			if (n == i++) {
				return it.next()
			}
			it.next()
		}
		val size = if (elements is Collection<*>)
			(elements as Collection<E>).size
		else
			elements.javaClass.simpleName
		throw IndexOutOfBoundsException("Out of bounds: $max > size ($size)")
	}

	/**
	 * @param elements the [SortedMap]
	 * @return the next random element
	 */
	fun <K, V> nextEntry(elements: SortedMap<K, V>): Map.Entry<K, V> {
		return nextElement(elements.entries, elements.size.toLong())
	}

	/**
	 * **NOTE** if the elements are not ordered, e.g. a [HashMap], then
	 * results are not guaranteed to be reproducible
	 *
	 * @param elements the [Map]
	 * @return the next random element
	 */
	fun <K, V> nextEntry(elements: Map<K, V>): Map.Entry<K, V> {
		return nextElement(elements.entries, elements.size.toLong())
	}

	companion object {

		private var nextNextGaussian = 0.0
		private var haveNextNextGaussian = false

		/** @see Random.nextGaussian */
		@Synchronized
		fun nextGaussian(rng: PseudoRandom): Double {
			// See Knuth, ACP, Section 3.4.1 Algorithm C.
			return if (haveNextNextGaussian) {
				haveNextNextGaussian = false
				nextNextGaussian
			} else {
				var v1: Double
				var v2: Double
				var s: Double
				do {
					v1 = 2 * rng.nextDouble() - 1 // between -1 and 1
					v2 = 2 * rng.nextDouble() - 1 // between -1 and 1
					s = v1 * v1 + v2 * v2
				} while (s >= 1 || s == 0.0)
				val multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s)
				nextNextGaussian = v2 * multiplier
				haveNextNextGaussian = true
				v1 * multiplier
			}
		}
	}

}