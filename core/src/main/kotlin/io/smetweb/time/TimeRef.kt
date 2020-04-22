package io.smetweb.time

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.smetweb.math.decimalValue
import io.smetweb.math.parseQuantity
import io.smetweb.math.toQuantity
import io.smetweb.ref.Ref
import si.uom.NonSI
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.MetricPrefix
import tec.uom.se.unit.Units
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.time.temporal.UnsupportedTemporalTypeException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Time
import javax.measure.spi.ServiceProvider

/**
 * [TimeRef] is a [Supplier] that wraps some measurable amount or
 * [ComparableQuantity] of [Time], referencing e.g.
 * <li>some time span (defined on a ratio scale); or</li>
 * <li>some time point (defined on an interval scale,
 * relative to some external absolute epoch like [Instant.EPOCH])</li>
 */
@FunctionalInterface
interface TimeRef:
		Ref<ComparableQuantity<Time>> // TODO allow [Quantity] types [Time] AND [Dimensionless]?
{

	/**
	 * [ConcreteOrdinal] is a separated interface for defining [TimeRef]
	 * as [Comparable] on any other [TimeRef] subtype
	 */
	interface ConcreteOrdinal<in T: ConcreteOrdinal<T>>: TimeRef, Comparable<T> {
		override fun compareTo(other: T): Int = get().compareTo(other.get())
	}

	/**
	 * [Ordinal] is a simple concrete data type that implements [Ref.Ordinal]
	 * thus rendering this type [Comparable] on any other [TimeRef] subtype
	 */
	data class Ordinal(
			override val value: ComparableQuantity<Time>
	): /*ConcreteOrdinal<Ordinal>,*/
			TimeRef, Ref.Ordinal<ComparableQuantity<Time>, ComparableQuantity<Time>> {

		@JsonCreator
		constructor(json: String): this(json.parseQuantity(Time::class.java))

		@JsonValue
		override fun toString(): String = get().toString()
	}

	fun decimalValue(): BigDecimal = decimalValue(get())

	fun decimalValue(unit: Unit<Time>): BigDecimal = decimalValue(get().to(unit))

	fun decimalUnits(): BigDecimal = decimalValue(BASE_UNIT)

	/** @return a 64-bit long value, possibly truncated as per [BigDecimal.toLong] */
	fun longValue(): Long = decimalValue().toLong()

	/** @return a 64-bit long value, possibly truncated as per [BigDecimal.toLong] */
	fun longValue(unit: TimeUnit): Long = decimalValue(get().to(resolveUnit(unit))).toLong()

	@Throws(ArithmeticException::class)
	fun longValueExact(): Long = decimalValue().longValueExact()

	@Throws(ArithmeticException::class)
	fun toDuration(): Duration {
		val seconds = decimalValue(SECOND)
		val secondsTruncated = seconds.toLong()
		val nanoAdjustment = seconds
				.subtract(BigDecimal(secondsTruncated))
				.scaleByPowerOfTen(9)
				.longValueExact()
		return Duration.ofSeconds(secondsTruncated, nanoAdjustment)
	}

	@Throws(ArithmeticException::class)
	fun toDuration(unit: TemporalUnit): Duration = Duration.of(decimalValue(resolveUnit(unit)).longValueExact(), unit)

	@Throws(ArithmeticException::class)
	fun toDate(epoch: Date = Date.from(Instant.EPOCH)): Date = Date(epoch.time + decimalValue(MILLISECOND).longValueExact())

	@Throws(ArithmeticException::class)
	fun toInstant(epoch: Instant = Instant.EPOCH): Instant = epoch.plus(toDuration())

	companion object {

		@JvmStatic
		val BASE_UNIT = Units.SECOND!!

		@JvmStatic
		val ZERO_UNITS = decimalUnits(BigDecimal.ZERO)

		@JvmStatic
		val T_ZERO = of(ZERO_UNITS)

		@JvmStatic
		val TIME_QUANTITY_FACTORY = ServiceProvider.current().getQuantityFactory(Time::class.java)!!

		@JvmStatic
		val SECOND = Units.SECOND!!

		@JvmStatic
		val MILLISECOND = MetricPrefix.MILLI(SECOND)!!

		@JvmStatic
		val MICROSECOND = MetricPrefix.MICRO(SECOND)!!

		@JvmStatic
		val NANOSECOND = MetricPrefix.NANO(SECOND)!!

		@JvmStatic
		val HALF_DAY = Units.DAY.divide(2.0)!!

		@JvmStatic
		fun of(value: ComparableQuantity<Time>) = Ordinal(value)

		@JvmStatic
		fun of(value: Quantity<Time>) = of(value.value.toQuantity(value.unit))

		@JvmStatic
		fun of(value: Number, unit: Unit<Time>) = of(value.toQuantity(unit))

		@JvmStatic
		fun of(value: Number, unit: TimeUnit) = of(value, resolveUnit(unit))

		@JvmStatic
		fun of(value: Number, unit: TemporalUnit) = of(value, resolveUnit(unit))

		@JvmStatic
		fun of(value: Date, epoch: Date = Date.from(Instant.EPOCH)) = of(value.time - epoch.time, MILLISECOND)

		@JvmStatic
		fun of(value: Instant, epoch: Instant = Instant.EPOCH) = of(Duration.between(epoch, value))

		@JvmStatic
		fun of(value: Duration) = of(value.toQuantity())

		@JvmStatic
		fun decimalUnits(value: Number) = value.toQuantity(BASE_UNIT)

		@JvmStatic
		fun decimalValue(t: Quantity<Time>) = t.decimalValue()

		@JvmStatic
		fun resolveUnit(unit: TimeUnit): Unit<Time> =
				unit.toUnit()

		@JvmStatic
		@Throws(DateTimeException::class)
		fun resolveTimeUnit(unit: Unit<Time>): TimeUnit =
				unit.toTimeUnit()

		@JvmStatic
		@Throws(UnsupportedTemporalTypeException::class)
		fun resolveUnit(unit: TemporalUnit): Unit<Time> =
				unit.toUnit()

		@JvmStatic
		@Throws(UnsupportedTemporalTypeException::class)
		fun resolveTemporalUnit(unit: Unit<Time>): TemporalUnit =
				unit.toTemporalUnit()
	}
}