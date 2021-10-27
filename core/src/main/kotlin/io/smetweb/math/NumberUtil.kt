package io.smetweb.math

import org.apfloat.Apfloat
import org.apfloat.ApfloatMath
import org.apfloat.ApfloatRuntimeException
import org.apfloat.Apint
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.time.ZonedDateTime
import java.util.Comparator
import kotlin.experimental.and

/**
 * TODO make configurable
 * @see [MathContext.DECIMAL64]
 */
val DEFAULT_CONTEXT: MathContext = MathContext.DECIMAL64

/** 5E-1 or 0.5  */
val ONE_HALF: BigDecimal = BigDecimal.valueOf(5, 1)

/** 1E3 or 1,000  */
val KILO: BigDecimal = BigDecimal.TEN.pow(3)

/** 1E6 or 1,000,000  */
val MEGA: BigDecimal = BigDecimal.TEN.pow(6)

/**
 * Java's [Math.E] Euler's number, achievable by summing to 1/18! and
 * scaling to 15 digits: [Number.scale] `(` [euler] `(18), 15)`
 */
val E: BigDecimal = BigDecimal.valueOf(Math.E)

/**  */
val TWO: Apfloat = Apint(2)

/**  */
const val hexFF: Byte = 0xFF.toByte()

fun <T: Comparable<T>> minOf(vararg values: T): T =
	values.minWithOrNull(Comparator.naturalOrder()) ?: error("Empty argument?")

fun <T: Comparable<T>> maxOf(vararg values: T): T =
	values.maxWithOrNull(Comparator.naturalOrder()) ?: error("Empty argument?")

/**
 * @return `true` iff the [BigDecimal] has scale `<=0`
 * @see [stackoverflow
 * discussion](http://stackoverflow.com/a/12748321)
 */
fun BigDecimal.isExact(): Boolean =
	this.signum() == 0 || this.scale() <= 0 || this.stripTrailingZeros().scale() <= 0

/**
 * @return `true` iff the [Apfloat] has scale `<=0` (http://stackoverflow.com/a/12748321)
 */
fun Apfloat.isExact(): Boolean =
	this.signum() == 0 || this.truncate() == this

/**
 * @return `true` iff the [Number] has scale `<=0` (http://stackoverflow.com/a/12748321)
 */
fun Number.isExact(): Boolean =
	when(this) {
		is Long, is Int, is Short, is Byte, is BigInteger, is Apint -> true
		is Apfloat -> isExact()
		else -> toDecimal().isExact()
	}

/**
 * @param scale
 * @return a [String] representation of scaled value with
 * [DEFAULT_CONTEXT] rounding mode
 */
fun Number.toString(scale: Int): String =
	this.toDecimal().setScale(scale, DEFAULT_CONTEXT.roundingMode).toPlainString()

/**
 * adopted from https://forum.processing.org/two/discussion/10384/bigdecimal-to-byte-array
 *
 * @return raw big-endian two's-complement binary representation with first
 * 4 bytes representing the scale.
 *
 * @see BigInteger.toByteArray
 * @see BigDecimal.unscaledValue
 */
fun Number.toByteArray(): ByteArray {
	val num: BigDecimal = this.toDecimal()
	val scale = num.scale()
	val scaleBytes = byteArrayOf((scale ushr 24).toByte(),
			(scale ushr 16).toByte(), (scale ushr 8).toByte(), scale.toByte())
	val unscaled = BigInteger(
			num.unscaledValue().toString())
	val unscaledBytes = unscaled.toByteArray()
	val concat = scaleBytes.copyOf(unscaledBytes.size + scaleBytes.size)
	System.arraycopy(unscaledBytes, 0, concat, scaleBytes.size,
			unscaledBytes.size)
	return concat
}

/**
 * adopted from https://forum.processing.org/two/discussion/10384/bigdecimal-to-byte-array
 *
 * big-endian two's-complement binary representation, with first 4 bytes representing the scale.
 *
 * @return a [BigDecimal]
 * @see BigInteger
 * @see BigDecimal
 */
fun ByteArray.toDecimal(): BigDecimal {
	// read scale from first 4 bytes
	val scale: Int = (
		(this[0] and hexFF).toInt() shl 24) or (
		(this[1] and hexFF).toInt() shl 16) or (
		(this[2] and hexFF).toInt() shl 8) or (
		(this[3] and hexFF).toInt())
	val subset = this.copyOfRange(4, this.size)
	val unscaled = BigInteger(subset)
	return BigDecimal(unscaled, scale)
}

@Throws(ApfloatRuntimeException::class)
fun Number.toApfloat(): Apfloat =
	when(this) {
		is Apfloat -> this
		is BigDecimal -> Apfloat(this)
		is Double -> Apfloat(this)
		is Float -> Apfloat(this)
		is Long, is Int, is Short, is Byte -> Apfloat(this as Long)
		is BigInteger -> Apfloat(this)
		else -> Apfloat(this.toDecimal())
	}

/**
 * TODO test with [kotlin.Number] vs [java.lang.Number] subtypes, e.g. [BigInteger] and [Apfloat]
 */
@Throws(NumberFormatException::class)
fun Number.toDecimal(mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
	when(this) {
		is BigDecimal -> this
		is Int -> this.toBigDecimal(mathContext = mathContext)
		is Byte -> this.toInt().toBigDecimal(mathContext = mathContext)
		is Long -> this.toBigDecimal(mathContext = mathContext)
		is Double -> this.toBigDecimal(mathContext = mathContext)
		is BigInteger -> this.toBigDecimal(mathContext = mathContext)
		is Apint -> BigDecimal(this.toBigInteger(), mathContext)
		else /* Apfloat, etc. */ -> this.toString().parseDecimal(mathContext = mathContext)
	}

/** see [BigDecimal] */
@Throws(ArithmeticException::class, NumberFormatException::class)
fun CharSequence.parseDecimal(mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
	BigDecimal(this.trim().toString(), mathContext)

/**
 * for difference between scale (decimals) and precision (significance), see
 * e.g. http://stackoverflow.com/a/13461270
 *
 * @param scale the number of decimals with [DEFAULT_CONTEXT]
 * rounding mode
 * @see BigDecimal.setScale
 */
fun Number.scale(scale: Int, mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
	this.toDecimal(mathContext).setScale(scale, mathContext.roundingMode)

fun Number.roundToInteger(mathContext: MathContext = DEFAULT_CONTEXT): BigInteger =
	this.scale(0, mathContext).toBigIntegerExact()

/**
 * @return the rounded [Int] value
 * @see BigDecimal.setScale
 */
@Throws(ArithmeticException::class)
fun Number.roundToInt(mathContext: MathContext = DEFAULT_CONTEXT): Int =
	when (this) {
		is Int, is Short, is Byte -> this.toInt()
		else -> this.roundToInteger(mathContext).intValueExact()
	}

/**
 * @return the rounded [Long] value
 * @see BigDecimal.setScale
 */
@Throws(ArithmeticException::class)
fun Number.roundToLong(mathContext: MathContext = DEFAULT_CONTEXT): Long =
		when (this) {
			is Long, is Int, is Short, is Byte -> this.toLong()
			else -> this.roundToInteger(mathContext).longValueExact()
		}

/**
 * [Binary (information) entropy](https://www.wikiwand.com/en/Binary_entropy_function)
 *
 * @param probabilities p_i(x)
 * @return H(X) = -SUM_i p(x) * log_2 p(x)
 */
fun binaryEntropy(vararg probabilities: Apfloat): Apfloat =
	Apfloat.ZERO.subtract(probabilities
			.filter { it != Apint.ZERO }
			.map { it.multiply(ApfloatMath.log(it, TWO)) }
			.reduce(Apfloat::add))

/**
 * [Binary (information) entropy](https://www.wikiwand.com/en/Binary_entropy_function)
 * of Bernoulli process (coin flip: p v. 1-p)
 *
 * @return H(X) = -SUM_x p(x) * log_2 p(x)
 */
fun Apfloat.binaryEntropy(): Apfloat =
	binaryEntropy(this, Apfloat.ONE.subtract(this))

/**
 * [Binary (information) entropy](https://www.wikiwand.com/en/Binary_entropy_function)
 *
 * @param probabilities p_i(x)
 * @return H(X) = -SUM_i p(x) * log_2 p(x)
 */
fun binaryEntropy(mathContext: MathContext = DEFAULT_CONTEXT, vararg probabilities: Number): BigDecimal =
	BigDecimal.ZERO.subtract(probabilities
			.filter { it != BigDecimal.ZERO }
			.map { it.toApfloat() }
			.map { it.multiply(ApfloatMath.log(it, TWO)) }
			.map { it.toDecimal(mathContext) }
			.reduce(BigDecimal::add))

/**
 * [Binary (information) entropy](https://www.wikiwand.com/en/Binary_entropy_function)
 * of bernoulli process (coin flip: p v. 1-p)
 *
 * @return H(X) = -SUM_x p(x) * log_2 p(x)
 */
fun Number.binaryEntropy(mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
	binaryEntropy(this.toApfloat()).toDecimal(mathContext)

/**
 * @return [this] degrees to radians
 */
@Throws(ArithmeticException::class)
fun Number.toRadians(mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
		try {
			ApfloatMath.toRadians(this.toApfloat()).toDecimal(mathContext)
		} catch(e: ApfloatRuntimeException) {
			throw ArithmeticException(e.message)
		}

/**
 * @return [this] radians to degrees
 */
@Throws(ArithmeticException::class)
fun Number.toDegrees(mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
		try {
			ApfloatMath.toDegrees(this.toApfloat()).toDecimal(mathContext)
		} catch(e: ApfloatRuntimeException) {
			throw ArithmeticException(e.message)
		}

/**
 * @return the [BigDecimal] opposite with [mathContext] precision
 */
fun Number.opposite(mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
	BigDecimal.ZERO.subtract(this.toDecimal(), mathContext)

/**
 * @param subtrahend
 * @return the [BigDecimal] subtraction with [mathContext] precision
 */
fun Number.subtract(subtrahend: Number, mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
		this.toDecimal().subtract(subtrahend.toDecimal(), mathContext)

/**
 * @param augend
 * @return the [BigDecimal] addition with [mathContext] precision
 */
fun Number.add(augend: Number, mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
		this.toDecimal().add(augend.toDecimal(), mathContext)

/**
 * @param multiplicand
 * @return the [BigDecimal] multiplication with [mathContext] precision
 */
fun Number.multiplyBy(multiplicand: Number, mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
		this.toDecimal().multiply(multiplicand.toDecimal(), mathContext)

/**
 * @param divisor the denominator
 * @return the [BigDecimal] division with [mathContext] precision
 */
fun Number.divideBy(divisor: Number, mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
		this.toDecimal().divide(divisor.toDecimal(), mathContext)

/**
 * @return 1/value with [mathContext] precision
 */
fun Number.inverse(mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
		BigDecimal.ONE.divideBy(this, mathContext)

fun Number.floor(mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
		when (this) {
			::isExact -> this.toDecimal(mathContext)
			is Apfloat -> this.floor().toDecimal(mathContext)
			else -> this.toDecimal(mathContext).setScale(0, RoundingMode.FLOOR)
		}

fun Number.ceil(mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
		when (this) {
			::isExact -> this.toDecimal(mathContext)
			is Apfloat -> this.ceil().toDecimal(mathContext)
			else -> this.toDecimal(mathContext).setScale(0, RoundingMode.CEILING)
		}

/**
 * @return
 * @see Apfloat.signum
 * @see BigDecimal.signum
 */
fun Number.signum(mathContext: MathContext = DEFAULT_CONTEXT): Int =
		when(this) {
			is Apfloat -> this.signum()
			else -> this.toDecimal(mathContext).signum()
		}

fun Number.isZero(mathContext: MathContext = DEFAULT_CONTEXT): Boolean =
		this.signum(mathContext) == 0

/**
 * @return the absolute (non-negative) value
 * @see Math.abs
 * @see ApfloatMath.abs
 * @see BigDecimal.abs
 */
fun Number.abs(mathContext: MathContext = DEFAULT_CONTEXT): Number =
	when(this) {
		is Apfloat -> ApfloatMath.abs(this)
		is Int -> kotlin.math.abs(this)
		is Long -> kotlin.math.abs(this)
		is Float -> kotlin.math.abs(this)
		is Double -> kotlin.math.abs(this)
		else -> this.toDecimal().abs(DEFAULT_CONTEXT)
	}

fun Number.sqrt(): Apfloat =
		this.root(2)

fun Number.root(n: Long): Apfloat =
	ApfloatMath.root(this.toApfloat(), n)

fun Number.pow(exponent: Number, mathContext: MathContext = DEFAULT_CONTEXT): Apfloat =
	when(exponent) {
		is Int, is Short, is Byte -> this.pow(exponent as Int, mathContext).toApfloat()
		is Long -> ApfloatMath.pow(this.toApfloat(), exponent)
		else -> ApfloatMath.pow(this.toApfloat(), exponent.toApfloat())
	}

/**
 * @param exponent
 * @return the power of value raised to exponent (with [DEFAULT_CONTEXT] precision for non-[Apfloat]s)
 */
fun Number.pow(exponent: Int, mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
	when(this) {
		is Apfloat -> ApfloatMath.pow(this.toApfloat(), exponent.toLong()).toDecimal(mathContext)
		else -> this.toDecimal(mathContext).pow(exponent, DEFAULT_CONTEXT)
	}

/**
 * convert the POSIX [ZonedDateTime] time stamp (seconds + nanos)
 * @return the rounded milliseconds
 */
fun ZonedDateTime.roundToMillis(mathContext: MathContext = DEFAULT_CONTEXT): BigInteger =
	this.second
		.multiplyBy(KILO, mathContext)
		.add(this.nano.divideBy(MEGA, mathContext))
		.roundToInteger(mathContext)

fun Long.factorial(): BigInteger =
	(2 until this)
		.map(BigInteger::valueOf)
		.reduce(BigInteger::multiply)
		.or(BigInteger.ONE)

fun euler(factorial: Int, mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal {
	var iFactorial = BigDecimal.ONE
	var e = BigDecimal.ONE
	var time = System.currentTimeMillis()
	var dt: Long
	for (i in 1 until factorial) {
		if (System.currentTimeMillis() - time.also { dt = it } >= 1000) {
			System.err.println("Calculating Euler, factorial " + i + " of "
					+ factorial + ", n!-precision: "
					+ iFactorial.precision() + ", e-scale: " + e.scale())
			time += dt
		}
		iFactorial = iFactorial.multiply(i.toDecimal(mathContext))
		e = e.add(iFactorial.inverse(mathContext))
	}
	return e
}

fun exp(exponent: Number, mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
	E.pow(exponent).toDecimal(mathContext)

fun exp(exponent: Number, factorial: Int, mathContext: MathContext = DEFAULT_CONTEXT): BigDecimal =
	euler(factorial).pow(exponent).toDecimal(mathContext)
