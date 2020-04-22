package io.smetweb.math

//import tec.uom.se.quantity.Quantities
import org.apfloat.Apfloat
import org.apfloat.ApfloatMath
import org.apfloat.ApfloatRuntimeException
import org.apfloat.Apint
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.time.ZonedDateTime
import kotlin.experimental.and

/**
 * TODO make configurable
 * @see [MathContext.DECIMAL64]
 */
val DEFAULT_CONTEXT: MathContext = MathContext.DECIMAL64

/** 5E-1 or 0.5  */
val ONE_HALF = BigDecimal.valueOf(5, 1)!!

/** 1E3 or 1,000  */
val KILO = BigDecimal.TEN.pow(3)!!

/** 1E6 or 1,000,000  */
val MEGA = BigDecimal.TEN.pow(6)!!

/**
 * Java's [Math.E] Euler's number, achievable by summing to 1/18! and
 * scaling to 15 digits: [Number.scale] `(` [euler] `(18), 15)`
 */
val E: BigDecimal = BigDecimal.valueOf(Math.E)!!

/**  */
private val TWO: Apfloat = Apint(2)

/**  */
const val hexFF: Byte = 0xFF.toByte()

/**
 * @return `true` iff the [BigDecimal] has scale `<=0`
 * @see [stackoverflow
 * discussion](http://stackoverflow.com/a/12748321)
 */
fun BigDecimal.isExact(): Boolean =
		this.signum() == 0 || this.scale() <= 0 || this.stripTrailingZeros().scale() <= 0

/**
 * @return `true` iff the [BigDecimal] has scale `<=0` (http://stackoverflow.com/a/12748321)
 */
fun Apfloat.isExact(): Boolean =
		this.signum() == 0 || this.truncate() == this

/**
 * @param value
 * @return
 */
fun Number.isExact(): Boolean =
		when(this) {
			is Long, is Int, is Short, is Byte, is BigInteger, is Apint -> true
			is Apfloat -> this.isExact()
			else -> this.toDecimal().isExact()
		}

/**
 * @param value
 * @param scale
 * @return a [String] representation of scaled value with
 * [DEFAULT_CONTEXT] rounding mode
 */
fun Number.toString(scale: Int): String =
		this.toDecimal().setScale(scale, DEFAULT_CONTEXT.roundingMode).toPlainString()

/**
 * adopted from https://forum.processing.org/two/discussion/10384/bigdecimal-to-byte-array
 *
 * @param num the [BigDecimal] to encode
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
 * @param raw big-endian two's-complement binary representation, with first 4
 * bytes representing the scale.
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
fun Number.toDecimal(): BigDecimal =
		when(this) {
			is BigDecimal -> this
			is Int -> this.toBigDecimal()
			is Byte -> this.toInt().toBigDecimal()
			is Long -> this.toBigDecimal(mathContext = DEFAULT_CONTEXT)
			is Double -> this.toBigDecimal(mathContext = DEFAULT_CONTEXT)
			is BigInteger -> this.toBigDecimal(mathContext = DEFAULT_CONTEXT)
			is Apint -> BigDecimal(this.toBigInteger(), DEFAULT_CONTEXT)
			else /* Apfloat, etc. */ -> this.toString().parseDecimal()
		}

// covers String, etc.
fun CharSequence.parseDecimal(): BigDecimal =
		BigDecimal(this.toString(), DEFAULT_CONTEXT)

/**
 * for difference between scale (decimals) and precision (significance), see
 * e.g. http://stackoverflow.com/a/13461270
 *
 * @param scale the number of decimals with [DEFAULT_CONTEXT]
 * rounding mode
 * @see BigDecimal.setScale
 */
fun Number.scale(scale: Int): BigDecimal =
		this.toDecimal().setScale(scale, DEFAULT_CONTEXT.roundingMode)

fun Number.round(): BigInteger =
		this.scale(0).toBigIntegerExact()

/**
 * @return the rounded [Int] value
 * @see BigDecimal.setScale
 */
@Throws(ArithmeticException::class)
fun Number.roundToInt(): Int =
		when (this) {
			is Int, is Short, is Byte -> this.toInt()
			else -> this.round().intValueExact()
		}

/**
 * @return the rounded [Long] value
 * @see BigDecimal.setScale
 */
@Throws(ArithmeticException::class)
fun Number.roundToLong(): Long =
		when (this) {
			is Long, is Int, is Short, is Byte -> this.toLong()
			else -> this.round().longValueExact()
		}

/**
 * Binary (information) entropy
 *
 * @param p(x)
 * @return H(X) = -SUM_x p(x) * log_2 p(x)
 * @see https://www.wikiwand.com/en/Binary_entropy_function
 */
fun binaryEntropy(vararg probabilities: Apfloat): Apfloat =
	Apfloat.ZERO.subtract(probabilities
			.filter { it != Apint.ZERO }
			.map { it.multiply(ApfloatMath.log(it, TWO)) }
			.reduce(Apfloat::add))

fun binaryEntropy(vararg probabilities: Number): BigDecimal =
	BigDecimal.ZERO.subtract(probabilities
			.filter { it != BigDecimal.ZERO}
			.map { it.toApfloat() }
			.map { it.multiply(ApfloatMath.log(it, TWO)) }
			.map { it.toDecimal() }
			.reduce(BigDecimal::add))

/**
 * Binary (information) entropy of Bernoulli process (coin flip: p v. 1-p)
 *
 * @param p(x)
 * @return H(X) = -SUM_x p(x) * log_2 p(x)
 * @see https://www.wikiwand.com/en/Binary_entropy_function
 */
fun Apfloat.binaryEntropy(): Apfloat =
	binaryEntropy(this, Apfloat.ONE.subtract(this))

/**
 * Binary (information) entropy of bernoulli process (coin flip: p v. 1-p)
 *
 * @param p(x)
 * @return H(X) = -SUM_x p(x) * log_2 p(x)
 * @see https://www.wikiwand.com/en/Binary_entropy_function
 */
fun Number.binaryEntropy(): BigDecimal =
	binaryEntropy(this.toApfloat()).toDecimal()

/**
 * @return [this] degrees to radians
 */
@Throws(ArithmeticException::class)
fun Number.toRadians(): BigDecimal =
		try {
			ApfloatMath.toRadians(this.toApfloat()).toDecimal()
		} catch(e: ApfloatRuntimeException) {
			throw ArithmeticException(e.message)
		}

/**
 * @return [this] radians to degrees
 */
@Throws(ArithmeticException::class)
fun Number.toDegrees(): BigDecimal =
		try {
			ApfloatMath.toDegrees(this.toApfloat()).toDecimal()
		} catch(e: ApfloatRuntimeException) {
			throw ArithmeticException(e.message)
		}

/**
 * @param subtrahend
 * @return the [BigDecimal] subtraction with [DEFAULT_CONTEXT] precision
 */
fun Number.subtractFrom(subtrahend: Number): BigDecimal =
		this.toDecimal().subtract(subtrahend.toDecimal(), DEFAULT_CONTEXT)

/**
 * @param augend
 * @return the [BigDecimal] addition with [DEFAULT_CONTEXT] precision
 */
fun Number.addTo(augend: Number): BigDecimal =
		this.toDecimal().add(augend.toDecimal(), DEFAULT_CONTEXT)

/**
 * @param multiplicand
 * @return the [BigDecimal] multiplication with [DEFAULT_CONTEXT] precision
 */
fun Number.multiplyBy(multiplicand: Number): BigDecimal =
		this.toDecimal().multiply(multiplicand.toDecimal(), DEFAULT_CONTEXT)

/**
 * @param divisor the denominator
 * @return the [BigDecimal] division with [DEFAULT_CONTEXT] precision
 */
fun Number.divideBy(divisor: Number): BigDecimal =
		this.toDecimal().divide(divisor.toDecimal(), DEFAULT_CONTEXT)

/**
 * @return 1/value with [DEFAULT_CONTEXT] precision
 */
fun Number.inverse(): BigDecimal =
		BigDecimal.ONE.divideBy(this)

fun Number.floor(): BigDecimal =
		when (this) {
			::isExact -> this.toDecimal()
			is Apfloat -> this.floor().toDecimal()
			else -> this.toDecimal().setScale(0, RoundingMode.FLOOR)
		}

fun Number.ceil(): BigDecimal =
		when (this) {
			::isExact -> this.toDecimal()
			is Apfloat -> this.ceil().toDecimal()
			else -> this.toDecimal().setScale(0, RoundingMode.CEILING)
		}

/**
 * @return
 * @see Apfloat.signum
 * @see BigDecimal.signum
 */
fun Number.signum(): Int =
		when(this) {
			is Apfloat -> this.signum()
			else -> this.toDecimal().signum()
		}

fun Number.isZero(): Boolean =
		this.signum() == 0

/**
 * @return the absolute (non-negative) value
 * @see Math.abs
 * @see ApfloatMath.abs
 * @see BigDecimal.abs
 */
fun Number.abs(): Number =
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

fun Number.pow(exponent: Number): Apfloat =
		when(exponent) {
			is Int, is Short, is Byte -> this.pow(exponent as Int).toApfloat()
			is Long -> ApfloatMath.pow(this.toApfloat(), exponent)
			else -> ApfloatMath.pow(this.toApfloat(), exponent.toApfloat())
		}

/**
 * @param exponent
 * @return the power of value raised to exponent (with [DEFAULT_CONTEXT] precision for non-[Apfloat]s)
 */
fun Number.pow(exponent: Int): BigDecimal =
		when(this) {
			is Apfloat -> ApfloatMath.pow(this.toApfloat(), exponent.toLong()).toDecimal()
			else -> this.toDecimal().pow(exponent, DEFAULT_CONTEXT)
		}

/**
 * @param value
 * @param exponent
 * @return the power of value raised to exponent
 */
fun pow(value: Apfloat?, exponent: Long): Number? {
	return pow(value as Apfloat?, exponent)
}

/**
 * @param posix the POSIX [ZonedDateTime] time stamp (seconds + nanos)
 * @return the rounded milliseconds
 */
fun ZonedDateTime.roundToMillis(): BigInteger =
		this.second
				.multiplyBy(KILO)
				.addTo(this.nano.divideBy(MEGA))
				.round()

fun Long.factorial(): BigInteger =
		(2 until this)
				.map(BigInteger::valueOf)
				.reduce(BigInteger::multiply)
				.or(BigInteger.ONE)

fun euler(factorial: Int): BigDecimal {
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
		iFactorial = iFactorial.multiply(i.toDecimal())
		e = e.add(iFactorial.inverse())
	}
	return e
}

fun exp(exponent: Number): BigDecimal =
		E.pow(exponent).toDecimal()

fun exp(exponent: Number, factorial: Int): BigDecimal =
		euler(factorial).pow(exponent).toDecimal()
