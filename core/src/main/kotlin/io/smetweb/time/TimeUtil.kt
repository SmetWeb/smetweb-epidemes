package io.smetweb.time

import io.smetweb.math.*
import org.joda.time.Period
import si.uom.NonSI
import tec.uom.se.ComparableQuantity
import tec.uom.se.quantity.Quantities
import tec.uom.se.unit.MetricPrefix
import tec.uom.se.unit.Units
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.time.temporal.UnsupportedTemporalTypeException
import java.util.concurrent.TimeUnit
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Time
import javax.measure.spi.ServiceProvider

@Deprecated("does not expressively support [tec.uom.se.ComparableQuantity]",
        replaceWith = ReplaceWith("tec.uom.se.quantity.Quantities.getQuantity"))
val TIME_QUANTITY_FACTORY = ServiceProvider.current().getQuantityFactory(Time::class.java)!!

val SECOND = Units.SECOND!!

val MILLISECOND = MetricPrefix.MILLI(SECOND)!!

val MICROSECOND = MetricPrefix.MICRO(SECOND)!!

val NANOSECOND = MetricPrefix.NANO(SECOND)!!

val HALF_DAY = Units.DAY.divide(2.0)!!

@Throws(ArithmeticException::class)
fun Quantity<Time>.value(unit: TimeUnit): Number =
        this.toUnit(unit).value

@Throws(ArithmeticException::class)
fun Quantity<Time>.value(unit: TemporalUnit): Number =
        this.toUnit(unit).value

@Throws(ArithmeticException::class)
fun Quantity<Time>.decimalValue(timeUnit: TimeUnit): BigDecimal =
        this.value(timeUnit).toDecimal()

@Throws(ArithmeticException::class)
fun Quantity<Time>.decimalValue(temporalUnit: TemporalUnit): BigDecimal =
        this.value(temporalUnit).toDecimal()

@Throws(ArithmeticException::class)
fun Quantity<Time>.decimalValue(timeUnit: TimeUnit, scale: Int): BigDecimal =
        this.value(timeUnit).scale(scale)

@Throws(ArithmeticException::class)
fun Quantity<Time>.decimalValue(temporalUnit: TemporalUnit, scale: Int): BigDecimal =
        this.value(temporalUnit).scale(scale)

fun Quantity<Time>.intValue(timeUnit: TimeUnit): Int =
        this.toUnit(timeUnit).intValue()

fun Quantity<Time>.intValue(temporalUnit: TemporalUnit): Int =
        this.toUnit(temporalUnit).intValue()

fun Quantity<Time>.longValue(timeUnit: TimeUnit): Long =
        this.toUnit(timeUnit).longValue()

fun Quantity<Time>.longValue(temporalUnit: TemporalUnit): Long =
        this.toUnit(temporalUnit).longValue()

fun Quantity<Time>.floatValue(timeUnit: TimeUnit): Float =
        this.toUnit(timeUnit).floatValue()

fun Quantity<Time>.floatValue(temporalUnit: TemporalUnit): Float =
        this.toUnit(temporalUnit).floatValue()

fun Quantity<Time>.doubleValue(timeUnit: TimeUnit): Double =
        this.toUnit(timeUnit).doubleValue()

fun Quantity<Time>.doubleValue(temporalUnit: TemporalUnit): Double =
        this.toUnit(temporalUnit).doubleValue()

fun Duration.toDecimalSeconds(): BigDecimal =
        this.seconds.toBigDecimal().add(this.nano.toBigDecimal().scaleByPowerOfTen(-9))

fun Duration.toQuantity(): ComparableQuantity<Time> =
        Quantities.getQuantity(this.toDecimalSeconds(), Units.SECOND)

@Throws(ArithmeticException::class)
fun Quantity<Time>.toDuration(): Duration {
    val seconds = this.decimalValue(TimeRef.SECOND)
    val secondsTruncated = seconds.toLong()
    val nanoAdjustment = seconds
            .subtract(secondsTruncated.toBigDecimal())
            .scaleByPowerOfTen(9)
            .longValueExact()
    return Duration.ofSeconds(secondsTruncated, nanoAdjustment)
}

fun Number.toQuantity(timeUnit: TimeUnit): ComparableQuantity<Time> =
        Quantities.getQuantity(this, timeUnit.toUnit())

fun Number.toQuantity(temporalUnit: TemporalUnit): ComparableQuantity<Time> =
        Quantities.getQuantity(this, temporalUnit.toUnit())

fun Quantity<Time>.toUnit(timeUnit: TimeUnit): ComparableQuantity<Time> =
        this.toUnit(timeUnit.toUnit())

fun Quantity<Time>.toUnit(temporalUnit: TemporalUnit): ComparableQuantity<Time> =
        this.toUnit(temporalUnit.toUnit())

fun TimeUnit.toUnit(): Unit<Time> =
        when(this) {
            TimeUnit.NANOSECONDS -> NANOSECOND
            TimeUnit.MICROSECONDS -> MICROSECOND
            TimeUnit.MILLISECONDS -> MILLISECOND
            TimeUnit.SECONDS -> Units.SECOND
            TimeUnit.MINUTES -> Units.MINUTE
            TimeUnit.HOURS -> Units.HOUR
            TimeUnit.DAYS -> Units.DAY
        }

@Throws(DateTimeException::class)
fun Unit<Time>.toTimeUnit(): TimeUnit =
        when(this) {
            NANOSECOND -> TimeUnit.NANOSECONDS
            MICROSECOND -> TimeUnit.MICROSECONDS
            MILLISECOND -> TimeUnit.MILLISECONDS
            Units.SECOND -> TimeUnit.SECONDS
            Units.MINUTE -> TimeUnit.MINUTES
            Units.HOUR -> TimeUnit.HOURS
            Units.DAY -> TimeUnit.DAYS
            else -> throw DateTimeException(
                    "Could not resolve $this as one of ${TimeUnit.values()}")
        }

@Throws(UnsupportedTemporalTypeException::class)
fun TemporalUnit.toUnit(): Unit<Time> =
        when(this) {
            ChronoUnit.NANOS -> NANOSECOND
            ChronoUnit.MICROS -> MICROSECOND
            ChronoUnit.MILLIS -> MILLISECOND
            ChronoUnit.SECONDS -> Units.SECOND
            ChronoUnit.MINUTES -> Units.MINUTE
            ChronoUnit.HOURS -> Units.HOUR
            ChronoUnit.HALF_DAYS -> HALF_DAY
            ChronoUnit.DAYS -> Units.DAY
            ChronoUnit.WEEKS -> Units.WEEK
            ChronoUnit.YEARS -> Units.YEAR
            else -> throw UnsupportedTemporalTypeException(
                    "Could not resolve $this as one of Non/SI ${Units::class.java}")
        }

@Throws(UnsupportedTemporalTypeException::class)
fun Unit<Time>.toTemporalUnit(): TemporalUnit =
        when(this) {
            NANOSECOND -> ChronoUnit.NANOS
            MICROSECOND -> ChronoUnit.MICROS
            MILLISECOND -> ChronoUnit.MILLIS
            Units.SECOND -> ChronoUnit.SECONDS
            Units.MINUTE -> ChronoUnit.MINUTES
            Units.HOUR -> ChronoUnit.HOURS
            Units.DAY -> ChronoUnit.DAYS
            HALF_DAY -> ChronoUnit.HALF_DAYS
            Units.WEEK -> ChronoUnit.WEEKS
            Units.YEAR -> ChronoUnit.YEARS
            NonSI.YEAR_CALENDAR -> ChronoUnit.YEARS
            NonSI.YEAR_JULIEN -> ChronoUnit.YEARS
            NonSI.YEAR_SIDEREAL -> ChronoUnit.YEARS
            else -> throw UnsupportedTemporalTypeException(
                    "Could not resolve $this as one of ${listOf(ChronoUnit.values())}")
        }

/**
 * Attempt parsing a [JSR-363][Quantity] time measurement, expecting
 * either:
 *
 *  * [Time] units (e.g. `"123 ms"`);
 *  * [Dimensionless] units (e.g. `"123 "`); or
 *  * ISO 8601 time period, parsed using
 * [JSR-310 Duration][java.time.Duration.parse] or
 * [Joda Period][Period.parse].
 *
 *
 * Examples of ISO 8601 time period:
 *
 * ```
 * "PT20.345S" -> parses as "20.345 seconds"
 * "PT15M"     -> parses as "15 minutes" (where a minute is 60 seconds)
 * "PT10H"     -> parses as "10 hours" (where an hour is 3600 seconds)
 * "P2D"       -> parses as "2 days" (where a day is 24 hours or 86400 seconds)
 * "P2DT3H4M"  -> parses as "2 days, 3 hours and 4 minutes"
 * "P-6H3M"    -> parses as "-6 hours and +3 minutes"
 * "-P6H3M"    -> parses as "-6 hours and -3 minutes"
 * "-P-6H+3M"  -> parses as "+6 hours and -3 minutes"
 * ```
 *
 * @param qty the [String] representation of a (relative) duration
 * @return a {@linkComparableQuantity}
 *
 * @see tec.uom.se.format.QuantityFormat.getInstance
 * @see java.time.Duration.parse
 * @see org.joda.time.format.ISOPeriodFormat.standard
 */
fun CharSequence.parseDuration(): ComparableQuantity<*> =
        try {
            this.parseQuantity()
        } catch (e: Exception) {
            try {
                Duration.parse(this).toQuantity()
            } catch (f: Exception) {
                try {
                    JodaUtil.toQuantity(JodaUtil.parsePeriod(this))
                } catch (g: Exception) {
                    throw IllegalArgumentException(
                            "Unable to parse '$this' with JSR-363: '" + parsedStringOrMessage(e)
                                    + "', JSR-310: '${f.message}', Joda: '${g.message}'")
                }
            }
        }

interface JodaUtil {

    /** separate (inner) class in case Joda library is unavailable (TODO test without Joda dependency) */
    companion object {

        @JvmStatic
        val THOUSAND: BigDecimal = BigDecimal.TEN.pow(3)

        @JvmStatic
        fun parsePeriod(value: CharSequence): Period =
                Period.parse(value.toString())

        @JvmStatic
        fun decimalSeconds(value: Period): BigDecimal =
                value.toStandardDuration().millis.toBigDecimal().divide(THOUSAND)

        @JvmStatic
        fun toQuantity(value: Period): ComparableQuantity<Time> =
                decimalSeconds(value).toQuantity(Units.SECOND)
    }

}