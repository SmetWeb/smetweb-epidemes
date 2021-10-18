package io.smetweb.time

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.smetweb.math.decimalValue
import io.smetweb.math.parseQuantity
import io.smetweb.math.toQuantity
import io.smetweb.refer.Ref
import io.smetweb.sim.dsol.DsolTimeRef
import tech.units.indriya.ComparableQuantity
import tech.units.indriya.unit.Units
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.TemporalUnit
import java.time.temporal.UnsupportedTemporalTypeException
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Time

/**
 * [TimeRef] is an (immutable) [Ref] of some [ComparableQuantity] of [Time], typically referencing either:
 * - some time span (defined on a ratio scale); or
 * - some time point (defined on an interval scale), relative to an external absolute epoch
 *   (e.g. [Instant.EPOCH] or [ClockService.epoch]).
 */
@FunctionalInterface
interface TimeRef: Ref<ComparableQuantity<Time>> // TODO allow [Quantity] types [Time] AND [Dimensionless]?
{

	/**
	 * work-around: compare by the smallest unit first, so larger unit value is multiplied (often more exact)
	 * reported to https://github.com/unitsofmeasurement/indriya/issues/356
	 */
	operator fun compareTo(other: TimeRef): Int =
		compareBySmallestUnit(other)

	/**
	 * [ConcreteOrdinal] is a simple concrete data type that implements [TimeRef], such that:
	 * - [compareTo] converts to the smallest unit, to avoid rounding-approximation errors
	 * - [equals] returns true also when [compareTo] returns `0`, i.e. even when units are of a different scale
	 */
	@Suppress("UNUSED")
	data class ConcreteOrdinal(
			override val value: ComparableQuantity<Time>
	): TimeRef, Ref.Ordinal<ComparableQuantity<Time>, ComparableQuantity<Time>> {

		@JsonCreator
		constructor(json: String): this(json.parseQuantity(Time::class.java))

		@JsonValue
		override fun toString(): String = value.toString()

		override fun equals(other: Any?): Boolean {
			val result = other?.let { that ->
				that is TimeRef
						&& (this.value == that.value
								|| this.compareTo(that) == 0)
			} ?: false
			return result
		}

		private val hashCode: Int by lazy { value.to(BASE_UNIT).decimalValue().hashCode() }

		override fun hashCode(): Int = hashCode

	}

	fun decimalValue(): BigDecimal =
		decimalValue(get())

	fun decimalValue(unit: Unit<Time>): BigDecimal =
		decimalValue(get().to(unit))

	fun decimalUnits(): BigDecimal =
		decimalValue(BASE_UNIT)

	/** @return a 64-bit long value, possibly truncated as per [BigDecimal.toLong] */
	fun longValue(): Long =
		decimalValue().toLong()

	/** @return a 64-bit long value, possibly truncated as per [BigDecimal.toLong] */
	fun longValue(unit: TimeUnit): Long =
		decimalValue(get().to(resolveUnit(unit))).toLong()

	@Throws(ArithmeticException::class)
	fun longValueExact(): Long =
		decimalValue().longValueExact()

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
	fun toDuration(unit: TemporalUnit): Duration =
		Duration.of(decimalValue(resolveUnit(unit)).longValueExact(), unit)

	@Throws(ArithmeticException::class)
	fun toDate(epoch: Date = Date.from(Instant.EPOCH)): Date =
		Date(epoch.time + decimalValue(MILLISECOND).longValueExact())

	@Throws(ArithmeticException::class)
	fun toInstant(epoch: Instant = Instant.EPOCH): Instant =
		epoch.plus(toDuration())

	companion object {

		@JvmStatic
		val BASE_UNIT: Unit<Time> = Units.SECOND

		@JvmStatic
		val ZERO_UNITS = decimalUnits(BigDecimal.ZERO)

		@JvmStatic
		val T_ZERO = of(ZERO_UNITS)

		@JvmStatic
		fun of(value: ComparableQuantity<Time>) =
			ConcreteOrdinal(value)

		@JvmStatic
		fun of(value: Quantity<Time>) =
			of(value.value.toQuantity(value.unit))

		@JvmStatic
		fun of(value: Number, unit: Unit<Time> = BASE_UNIT) =
			of(value.toQuantity(unit))

		@JvmStatic // ZonedDateTime
		fun of(value: Number, unit: TimeUnit) =
			of(value, resolveUnit(unit))

		@JvmStatic
		fun of(value: Number, unit: TemporalUnit) =
			of(value, resolveUnit(unit))

		@JvmStatic
		fun of(value: Date, epoch: Date = Date.from(Instant.EPOCH)) =
			of(value.time - epoch.time, MILLISECOND)

		@JvmStatic
		fun of(value: ZonedDateTime, epoch: Instant = Instant.EPOCH) =
			of(Duration.between(epoch, value))

		@JvmStatic
		fun of(value: Instant, epoch: Instant = Instant.EPOCH) =
			of(Duration.between(epoch, value))

		@JvmStatic
		fun of(value: Duration) =
			of(value.toQuantity())

		@JvmStatic
		fun decimalUnits(value: Number) =
			value.toQuantity(BASE_UNIT)

		@JvmStatic
		fun decimalValue(value: Quantity<Time>) =
			value.decimalValue()

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