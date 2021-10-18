package io.smetweb.time

import io.smetweb.math.*
import si.uom.NonSI
import tech.units.indriya.AbstractUnit
import tech.units.indriya.ComparableQuantity
import tech.units.indriya.quantity.Quantities
import tech.units.indriya.quantity.time.TimeQuantities
import tech.units.indriya.unit.Units
import java.math.BigDecimal
import java.time.*
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.time.temporal.UnsupportedTemporalTypeException
import java.util.concurrent.TimeUnit
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Time

val SECOND: Unit<Time> = Units.SECOND

val MILLISECOND: Unit<Time> = TimeQuantities.MILLISECOND

val MICROSECOND: Unit<Time> = TimeQuantities.MICROSECOND

val NANOSECOND: Unit<Time> = TimeQuantities.NANOSECOND

val HALF_DAY: Unit<Time> = Units.DAY.divide(2.0)

private val TIME_UNITS_IN_SECONDS = mutableMapOf<Unit<Time>, Double>()

private fun Unit<Time>.toSeconds() =
    synchronized (TIME_UNITS_IN_SECONDS) {
        TIME_UNITS_IN_SECONDS.computeIfAbsent(this) { it.getConverterTo(Units.SECOND).convert(1.0) }
    }

fun TimeRef.compareBySmallestUnit(other: TimeRef): Int {
    return if(this.get().unit.toSeconds() > other.get().unit.toSeconds()) {
        -1 * other.get().compareTo(get())
    } else {
        get().compareTo(other.get()) // no need to invert if this' unit is same or less than that's unit
    }
}

fun CharSequence.parseZonedDateTime(defaultZone: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
        try {
            // e.g. "2007-12-03T10:15:30+01:00[Europe/Paris]"
            ZonedDateTime.parse(this)
        } catch(e: DateTimeParseException) {
            try {
                // e.g. "2007-12-03T10:15:30+01:00"
                OffsetDateTime.parse(this).toZonedDateTime()
            } catch(e: DateTimeParseException) {
                try {
                    // e.g. "2007-12-03T10:15:30"
                    LocalDateTime.parse(this).atZone(defaultZone)
                } catch(e: DateTimeParseException) {
                    try {
                        // e.g. "2007-12-03"
                        LocalDate.parse(this).atStartOfDay(defaultZone)
                    } catch(e: DateTimeParseException) {
                        ZonedDateTime.now()
                    }
                }
            }
        }

@Throws(ArithmeticException::class)
fun Quantity<Time>.value(unit: TimeUnit): Number =
        this.to(unit).value

@Throws(ArithmeticException::class)
fun Quantity<Time>.value(unit: TemporalUnit): Number =
        this.to(unit).value

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
        this.to(timeUnit).intValue()

fun Quantity<Time>.intValue(temporalUnit: TemporalUnit): Int =
        this.to(temporalUnit).intValue()

fun Quantity<Time>.longValue(timeUnit: TimeUnit): Long =
        this.to(timeUnit).longValue()

fun Quantity<Time>.longValue(temporalUnit: TemporalUnit): Long =
        this.to(temporalUnit).longValue()

fun Quantity<Time>.floatValue(timeUnit: TimeUnit): Float =
        this.to(timeUnit).floatValue()

fun Quantity<Time>.floatValue(temporalUnit: TemporalUnit): Float =
        this.to(temporalUnit).floatValue()

fun Quantity<Time>.doubleValue(timeUnit: TimeUnit): Double =
        this.to(timeUnit).doubleValue()

fun Quantity<Time>.doubleValue(temporalUnit: TemporalUnit): Double =
        this.to(temporalUnit).doubleValue()

fun Duration.toDecimalSeconds(): BigDecimal =
        this.seconds.toBigDecimal().add(this.nano.toBigDecimal().scaleByPowerOfTen(-9))

fun Duration.toQuantity(): ComparableQuantity<Time> =
        Quantities.getQuantity(this.toDecimalSeconds(), Units.SECOND)

@Throws(ArithmeticException::class)
fun Quantity<Time>.toDuration(): Duration {
    val seconds = this.decimalValue(SECOND)
    val secondsTruncated = seconds.toLong()
    val nanoAdjustment = seconds
            .subtract(secondsTruncated.toBigDecimal())
            .scaleByPowerOfTen(9)
            .longValueExact()
    return Duration.ofSeconds(secondsTruncated, nanoAdjustment)
}

fun <Q: Quantity<Q>> Number.toQuantity(unit: Unit<Q>): ComparableQuantity<Q> =
    Quantities.getQuantity(this, unit)

fun Number.toQuantity(timeUnit: TimeUnit): ComparableQuantity<Time> =
    toQuantity(timeUnit.toUnit())

fun Number.toQuantity(temporalUnit: TemporalUnit): ComparableQuantity<Time> =
    toQuantity(temporalUnit.toUnit())

fun Quantity<Time>.to(timeUnit: TimeUnit): ComparableQuantity<Time> =
    toUnit(timeUnit.toUnit())

fun Quantity<Time>.to(temporalUnit: TemporalUnit): ComparableQuantity<Time> =
    toUnit(temporalUnit.toUnit())

fun TimeUnit.toUnit(): Unit<Time> =
        when(this) {
            TimeUnit.NANOSECONDS -> TimeQuantities.NANOSECOND
            TimeUnit.MICROSECONDS -> TimeQuantities.MICROSECOND
            TimeUnit.MILLISECONDS -> TimeQuantities.MILLISECOND
            TimeUnit.SECONDS -> Units.SECOND
            TimeUnit.MINUTES -> Units.MINUTE
            TimeUnit.HOURS -> Units.HOUR
            TimeUnit.DAYS -> Units.DAY
        }

@Throws(DateTimeException::class)
fun Unit<Time>.toTimeUnit(): TimeUnit =
        when(this) {
            TimeQuantities.NANOSECOND -> TimeUnit.NANOSECONDS
            TimeQuantities.MICROSECOND -> TimeUnit.MICROSECONDS
            TimeQuantities.MILLISECOND -> TimeUnit.MILLISECONDS
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
            ChronoUnit.NANOS -> TimeQuantities.NANOSECOND
            ChronoUnit.MICROS -> TimeQuantities.MICROSECOND
            ChronoUnit.MILLIS -> TimeQuantities.MILLISECOND
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
            TimeQuantities.NANOSECOND -> ChronoUnit.NANOS
            TimeQuantities.MICROSECOND -> ChronoUnit.MICROS
            TimeQuantities.MILLISECOND -> ChronoUnit.MILLIS
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
 * Attempt parsing a [JSR-275/363/385][Quantity] time measurement, expecting
 * either:
 *  - [Time] units (e.g. `"123 ms"`);
 *  - [Dimensionless] units (e.g. `"123 "`); or
 *  - ISO 8601 time period, parsed using
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
 * @see tech.units.indriya.format.QuantityFormat.getInstance
 * @see java.time.Duration.parse
 * @see org.joda.time.format.ISOPeriodFormat.standard
 */
fun CharSequence.parseTimeQuantity(
    defaultUnit: Unit<Time> = TimeRef.BASE_UNIT,
    offset: Instant? = null,
): ComparableQuantity<Time> =
        try {
            this.parseQuantity().let {
                if(it.unit == AbstractUnit.ONE)
                    it.value.toQuantity(defaultUnit)
                else
                    it.asType(Time::class.java)
            }
        } catch (e: Exception) {
            try {
                Duration.parse(this).toQuantity()
            } catch (f: Exception) {
                try {
                    JodaUtil.toQuantity(JodaUtil.parsePeriod(this), offset?.toEpochMilli())
                } catch (g: Exception) {
                    throw IllegalArgumentException("Unable to parse '$this'" +
                            "\n\tjavax.measure: ${parsedStringOrMessage(e)}" +
                            "\n\t    java.time: ${parsedStringOrMessage(f)}" +
                            "\n\t    joda-time: ${g.message}")
                }
            }
        }

interface JodaUtil {

    /** separate (inner) class in case Joda library is unavailable (TODO test without Joda dependency) */
    companion object {

        @JvmStatic
        val THOUSAND: BigDecimal = BigDecimal.TEN.pow(3)

        @JvmStatic
        fun parsePeriod(value: CharSequence): org.joda.time.Period =
                org.joda.time.Period.parse(value.toString())

        @JvmStatic
        fun decimalSeconds(value: org.joda.time.Period, offset: Long? = null): BigDecimal =
                (offset?.let { value.toDurationFrom(org.joda.time.Instant.ofEpochMilli(it)) }
                        ?: value.toStandardDuration()).millis.toBigDecimal().divide(THOUSAND)

        @JvmStatic
        fun toQuantity(value: org.joda.time.Period, offset: Long? = null): ComparableQuantity<Time> =
                decimalSeconds(value, offset).toQuantity(Units.SECOND)
    }

}