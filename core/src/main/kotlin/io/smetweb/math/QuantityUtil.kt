package io.smetweb.math

import si.uom.NonSI
import tech.units.indriya.AbstractUnit
import tech.units.indriya.ComparableQuantity
import tech.units.indriya.format.FormatBehavior
import tech.units.indriya.format.NumberDelimiterQuantityFormat
import tech.units.indriya.function.DefaultNumberSystem
import tech.units.indriya.quantity.Quantities
import tech.units.indriya.unit.Units
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.ParsePosition
import java.util.Comparator
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.format.MeasurementParseException
import javax.measure.format.UnitFormat
import javax.measure.quantity.Dimensionless
import javax.measure.spi.ServiceProvider

val UNIT_FORMAT: UnitFormat = ServiceProvider.current().formatService.unitFormat

// provides tech.units.indriya.spi.NumberSystem with
//    	tech.units.indriya.function.DefaultNumberSystem;
val NUMBER_SYSTEM: DefaultNumberSystem = object: DefaultNumberSystem() {
    override fun narrow(number: Number): Number = number // disable narrowing, keep number type intact
}

const val VALUE_UNIT_SEPARATOR = ' '

val WHITESPACE_REGEX = "\\s+".toRegex()

//static
//{
//	// TODO add unit labels
//	SimpleUnitFormat.getInstance().label(Units.DEGREE_ANGLE, "deg")
//	QuantityJsonModule.checkRegistered(JsonUtil.getJOM())
//}

/** dimension one, for pure or [Dimensionless] quantities  */
val PURE: Unit<Dimensionless> = AbstractUnit.ONE

/** the number ZERO  */
val ZERO = BigDecimal.ZERO.toQuantity(PURE)

/** the number ONE  */
val ONE = BigDecimal.ONE.toQuantity(PURE)

/**
 * TODO test with [kotlin.Number] vs [java.lang.Number] subtypes
 */
fun <Q: Quantity<Q>> Number.toQuantity(unit: Unit<Q>): ComparableQuantity<Q> =
        Quantities.getQuantity(this, unit)

/**
 * @return a [ComparableQuantity]
 */
fun <Q : Quantity<Q>> Quantity<Q>.toQuantity(): ComparableQuantity<Q> =
        if (this.value is ComparableQuantity<*>)
            @Suppress("UNCHECKED_CAST")
            this as ComparableQuantity<Q>
        else
            this.value.toQuantity(this.unit)

fun Number.toQuantity(): ComparableQuantity<Dimensionless> =
        this.toQuantity(AbstractUnit.ONE)

//@Deprecated("remove when degree/radian conversions is fixed in JSR-363 uom-se",
//        replaceWith = ReplaceWith("Quantity.to"))
@Throws(ArithmeticException::class)
fun <Q : Quantity<Q>> Quantity<Q>.toUnit(unit: Unit<Q>): ComparableQuantity<Q> =
        if(!this.unit.isCompatible(unit)) {
            throw ArithmeticException("Incompatible ${this.unit} vs. $unit")
        }
        // special cases
        else if(this.unit === Units.RADIAN && unit == NonSI.DEGREE_ANGLE) {
            this.value.toDegrees().toQuantity(unit)
        } else if(unit === Units.RADIAN && this.unit == NonSI.DEGREE_ANGLE) {
            @Suppress("UNCHECKED_CAST")
            this.value.toRadians().toQuantity(unit) as ComparableQuantity<Q>
        }
        // default implementation
        else {
            this.to(unit) as ComparableQuantity<Q>
        }

/** String, etc. */
fun <Q: Quantity<Q>> CharSequence.parseQuantity(quantity: Class<Q>): ComparableQuantity<Q> =
        Quantities.getQuantity(this).asType(quantity) as ComparableQuantity<Q>

fun CharSequence.parseQuantity(): ComparableQuantity<*> =
    try {
        NumberDelimiterQuantityFormat.getInstance(FormatBehavior.LOCALE_NEUTRAL)
                .parse(this, ParsePosition(0)) as ComparableQuantity<*>
    } catch (e: MeasurementParseException) {
        throw IllegalArgumentException("Could not parse '${e.parsedString}'", e)
    }

@Suppress("UNCHECKED_CAST")
fun <Q: Quantity<Q>, R: Quantity<R>> CharSequence.parseQuantity(unitFormat: UnitFormat = UNIT_FORMAT): ComparableQuantity<R> {
    val split: List<String> = this.toString().trim().split(WHITESPACE_REGEX)
    return if (split.size < 2)
        split[0].parseDecimal().toQuantity(PURE) as ComparableQuantity<R>
    else
        split[0].parseDecimal().toQuantity(unitFormat.parse(split[1]) as Unit<R>)
}

@Deprecated("remove when exception is clarified in uom-se",
        replaceWith = ReplaceWith("ParserException.getMessage"))
fun parsedStringOrMessage(e: Throwable): String? =
        when (e) {
            is MeasurementParseException -> e.parsedString
            else -> if (e.cause is MeasurementParseException)
                (e.cause as MeasurementParseException).parsedString
            else
                e.message
            }

fun <Q: Quantity<Q>> Quantity<Q>.toPlainString(): String =
        this.decimalValue().toPlainString() + VALUE_UNIT_SEPARATOR + this.unit

fun <Q: Quantity<Q>> Quantity<Q>.toPlainString(scale: Int): String =
        this.decimalValue().setScale(scale, RoundingMode.HALF_UP).toPlainString() + VALUE_UNIT_SEPARATOR + this.unit

@Throws(ArithmeticException::class)
fun <Q: Quantity<Q>> Quantity<Q>.value(unit: Unit<Q>): Number =
        this.toUnit(unit).value

@Throws(NumberFormatException::class)
fun <Q: Quantity<Q>> Quantity<Q>.decimalValue(): BigDecimal =
        this.value.toDecimal()

@Throws(ArithmeticException::class)
fun <Q: Quantity<Q>> Quantity<Q>.decimalValue(unit: Unit<Q>): BigDecimal =
        this.value(unit).toDecimal()

@Throws(ArithmeticException::class)
fun <Q: Quantity<Q>> Quantity<Q>.decimalValue(unit: Unit<Q>, scale: Int): BigDecimal =
        this.value(unit).scale(scale)

/** @return `true` iff non-zero and non-positive */
@Throws(NumberFormatException::class)
fun <Q: Quantity<Q>> Quantity<Q>.isNegative(): Boolean =
        this.decimalValue().signum() < 0

/**
 * @return power [Quantity] in units of the same (exact) power
 * @see Number.pow
 */
@Suppress("UNCHECKED_CAST")
fun <Q: Quantity<Q>, R: Quantity<R>> Quantity<Q>.pow(exponent: Int): ComparableQuantity<R> =
        this.value.pow(exponent).toQuantity(this.unit.pow(exponent) as Unit<R>)

/**
 * @return power value in undefined units
 * @see Number.pow
 */
fun <Q: Quantity<Q>> Quantity<Q>.powValue(exponent: Number): Number =
        this.value.pow(exponent)

/**
 * applies [DEFAULT_CONTEXT] to avoid
 * [ArithmeticException] due to non-terminating decimal expansion
 *
 * @see Quantity.inverse
 * @see tech.units.indriya.quantity.DecimalQuantity
 */
@Suppress("UNCHECKED_CAST")
fun <Q: Quantity<Q>, R: Quantity<R>> Quantity<Q>.inverse(): ComparableQuantity<R> =
        this.value.inverse().toQuantity(this.unit.inverse() as Unit<R>)

/**
 * @see Number.isExact
 * @see Number.floor
 */
fun <Q: Quantity<Q>> Quantity<Q>.floor(): ComparableQuantity<Q> =
        when (this.value) {
            Number::isExact -> this.value.toQuantity(this.unit)
            else -> this.value.floor().toQuantity(this.unit)
        }

/**
 * @see Number.isExact
 * @see Number.floor
 */
fun <Q : Quantity<Q>> Quantity<Q>.ceil(): ComparableQuantity<Q> =
        when (this.value) {
            Number::isExact -> this.value.toQuantity(this.unit)
            else -> this.value.ceil().toQuantity(this.unit)
        }

/**
 * @see Number.abs
 */
fun <Q: Quantity<Q>> Quantity<Q>.abs(): ComparableQuantity<Q> =
        when(this) {
            ::isNegative -> this.value.abs().toQuantity(this.unit)
            else -> this.value.toQuantity(this.unit)
        }

/**
 * @return the square root [Quantity] value/unit
 * @see Quantity.root
 */
fun <Q: Quantity<Q>, R: Quantity<R>> Quantity<Q>.sqrt(): ComparableQuantity<R> =
        this.root(2)

/**
 * @return the root [Quantity] value/unit
 * @see Number.root
 * @see Unit.root
 */
@Suppress("UNCHECKED_CAST")
fun <Q: Quantity<Q>, R: Quantity<R>> Quantity<Q>.root(n: Int): ComparableQuantity<R> =
        this.value.root(n.toLong()).toQuantity(this.unit.root(n) as Unit<R>)

fun <Q: Quantity<Q>> Quantity<Q>.rootDecimal(n: Long): BigDecimal =
        this.value.root(n).toDecimal()

/**
 * @return [Quantity.getValue] as (truncated) [Int]
 * @see Number.toInt
 */
fun <Q: Quantity<Q>> Quantity<Q>.intValue(): Int =
        this.value.toInt()

/**
 * @return [Quantity.getValue] as (truncated) [Int]
 * @see Quantity.toUnit
 * @see Number.toInt
 */
fun <Q : Quantity<Q>> Quantity<Q>.intValue(unit: Unit<Q>): Int =
        this.toUnit(unit).intValue()

/**
 * @return [Quantity.getValue] as (truncated) [Long]
 * @see Number.toLong
 */
fun <Q: Quantity<Q>> Quantity<Q>.longValue(): Long =
        this.value.toLong()

/**
 * @return [Quantity.getValue] as (truncated) [Long]
 * @see Quantity.toUnit
 * @see Number.toLong
 */
fun <Q: Quantity<Q>> Quantity<Q>.longValue(unit: Unit<Q>): Long =
        this.toUnit(unit).longValue()

/**
 * @return [Quantity.getValue] as (truncated) [Float]
 * @see Number.toFloat
 */
fun <Q: Quantity<Q>> Quantity<Q>.floatValue(): Float =
        this.value.toFloat()

/**
 * @return [Quantity.getValue] as (truncated) [Float]
 * @see Quantity.toUnit
 * @see Number.toFloat
 */
fun <Q: Quantity<Q>> Quantity<Q>.floatValue(unit: Unit<Q>): Float =
        this.toUnit(unit).floatValue()

/**
 * @return [Quantity.getValue] as (truncated) [Double]
 * @see Number.toDouble
 */
fun <Q: Quantity<Q>> Quantity<Q>.doubleValue(): Double =
        this.value.toDouble()

/**
 * @return [Quantity.getValue] as (truncated) [Double]
 * @see Quantity.toUnit
 * @see Number.toDouble
 */
fun <Q: Quantity<Q>> Quantity<Q>.doubleValue(unit: Unit<Q>): Double =
        this.toUnit(unit).doubleValue()

fun <Q: Quantity<Q>> minOf(vararg quantities: Quantity<Q>): ComparableQuantity<Q> =
        quantities.map(Quantity<Q>::toQuantity).minWithOrNull(Comparator.naturalOrder()) ?: error("Empty argument?")

fun <Q: Quantity<Q>> maxOf(vararg quantities: Quantity<Q>): ComparableQuantity<Q> =
        quantities.map(Quantity<Q>::toQuantity).minWithOrNull(Comparator.naturalOrder()) ?: error("Empty argument?")

fun <Q: Quantity<Q>> Quantity<Q>.approximates(that: Quantity<Q>, precision: Int): Boolean =
        this.value.scale(precision - 1).compareTo(that.toUnit(this.unit).value.scale(precision - 1)) == 0

/**
 * @see Number.toDecimal
 * @see BigDecimal.precision
 */
fun <Q: Quantity<Q>> Quantity<Q>.precision(): Int =
        this.decimalValue().precision()

/**
 * @see Number.toDecimal
 * @see BigDecimal.scale
 */
fun <Q: Quantity<Q>> Quantity<Q>.scale(): Int =
        this.decimalValue().scale()

/**
 * @see Number.toDecimal
 * @see BigDecimal.scale
 */
fun <Q: Quantity<Q>> Quantity<Q>.scale(scale: Int): ComparableQuantity<Q> =
        this.decimalValue().scale(scale).toQuantity(this.unit)

/**
 * @see Quantity.toUnit
 * @see Quantity.scale
 */
fun <Q: Quantity<Q>> Quantity<Q>.scale(unit: Unit<Q>, scale: Int): ComparableQuantity<Q> =
        this.toUnit(unit).scale(scale)

/**
 * @see Number.toDecimal
 * @see BigDecimal.signum
 */
fun <Q: Quantity<Q>> Quantity<Q>.signum(): Int =
        this.decimalValue().signum()

private val UNIT_ZEROES: MutableMap<Unit<*>?, ComparableQuantity<*>> = HashMap()

private val UNIT_ONES: MutableMap<Unit<*>?, ComparableQuantity<*>> = HashMap()

/**
 * @param unit
 * @return zero
 */
@Suppress("UNCHECKED_CAST")
fun <Q: Quantity<Q>> zero(unit: Unit<Q>): ComparableQuantity<Q> =
        UNIT_ZEROES.computeIfAbsent(unit) { BigDecimal.ZERO.toQuantity(unit) } as ComparableQuantity<Q>

/**
 * @param unit
 * @return one
 */
@Suppress("UNCHECKED_CAST")
fun <Q: Quantity<Q>> one(unit: Unit<Q>): ComparableQuantity<Q> =
        UNIT_ONES.computeIfAbsent(unit) { BigDecimal.ONE.toQuantity(unit) } as ComparableQuantity<Q>

/**
 * @param dimension
 * @return zero
 */
fun <Q: Quantity<Q>> zero(dimension: Class<Q>): ComparableQuantity<Q> =
        zero(Units.getInstance().getUnit(dimension))

/**
 * @param dimension
 * @return one
 */
fun <Q: Quantity<Q>> one(dimension: Class<Q>): ComparableQuantity<Q> =
        one(Units.getInstance().getUnit(dimension))
