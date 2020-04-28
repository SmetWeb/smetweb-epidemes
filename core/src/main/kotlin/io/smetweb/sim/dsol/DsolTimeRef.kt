package io.smetweb.sim.dsol

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.smetweb.math.UNIT_FORMAT
import io.smetweb.math.VALUE_UNIT_SEPARATOR
import io.smetweb.math.toDecimal
import io.smetweb.math.toQuantity
import io.smetweb.time.TimeRef
import io.smetweb.time.toQuantity
import nl.tudelft.simulation.dsol.simtime.SimTime
import tec.uom.se.ComparableQuantity
import java.math.BigDecimal
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Time
import java.time.Duration
import java.time.Instant
import java.time.temporal.TemporalUnit
import java.util.*
import java.util.concurrent.TimeUnit

/** [DsolTimeRef] models virtual/simulated time, extending D-SOL's [SimTime] */
data class DsolTimeRef(
		private var absoluteTime: ComparableQuantity<Time>,
		override val value: ComparableQuantity<Time> = absoluteTime
): SimTime<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>(absoluteTime), TimeRef.ConcreteOrdinal<DsolTimeRef> {

	/** always convert to [BigDecimal], for consistent [hashCode] values among the various [Number] subtypes */
	constructor(value: Number, unit: Unit<Time>):
			this(value.toDecimal().toQuantity(unit))

	constructor(value: Number):
			this(value, TimeRef.BASE_UNIT)

	private constructor(time: List<String>): this(
			TimeRef.TIME_QUANTITY_FACTORY.create(
					BigDecimal(time[0]),
					when(time.size){
						1 -> TimeRef.BASE_UNIT
						2 -> UNIT_FORMAT.parse(time[1]).asType(Time::class.java)
						else -> throw IllegalArgumentException("Unable to parse model quantity from: $time")
					}
			))

	@JsonCreator
	constructor(time: String): this(time.trim().split(VALUE_UNIT_SEPARATOR))

	constructor(quantity: Quantity<Time>): this(quantity.toQuantity())

	constructor(duration: Duration): this(duration.toQuantity())

	override fun compareTo(other: DsolTimeRef): Int = get().compareTo(other.get())

	override fun set(value: ComparableQuantity<Time>) {
		this.absoluteTime = value
	}

	override fun get() = absoluteTime

	@JsonValue
	override fun toString() = get().toString()

	override fun equals(other: Any?): Boolean{
		if (this === other) return true
		if (other?.javaClass != javaClass) return false
		return get() == (other as DsolTimeRef).get()
	}

	private var hashCode: Int? = null

	override fun hashCode(): Int {
		if (hashCode == null) {
			hashCode = get().to(TimeRef.BASE_UNIT).value.hashCode()
		}
		return hashCode!!
	}

	override fun copy() = DsolTimeRef(get())

	override fun setZero() = T_ZERO

	override fun getAbsoluteZero() = TimeRef.ZERO_UNITS

	override fun getRelativeZero(): BigDecimal = BigDecimal.ZERO

	override fun add(durationBaseUnits: BigDecimal) {
		set(get().add(TimeRef.decimalUnits(durationBaseUnits)))
	}

	override fun subtract(durationBaseUnits: BigDecimal) {
		set(get().subtract(TimeRef.decimalUnits(durationBaseUnits)))
	}

	override fun diff(absoluteTime: ComparableQuantity<Time>): BigDecimal {
		return get().subtract(absoluteTime).to(TimeRef.BASE_UNIT).value as BigDecimal
	}

	companion object {

		@JvmStatic
		val T_ZERO: DsolTimeRef = DsolTimeRef(TimeRef.ZERO_UNITS)

		@JvmStatic
		fun of(value: ComparableQuantity<Time>): DsolTimeRef =
                DsolTimeRef(value)

		@JvmStatic
		fun of(value: Quantity<Time>): DsolTimeRef =
                of(value.toQuantity()) // convert to [ComparableQuantity]

		@JvmStatic
		fun of(value: Number, unit: Unit<Time>): DsolTimeRef =
                of(value.toQuantity(unit))

		@JvmStatic
		fun of(value: Number, unit: TimeUnit): DsolTimeRef =
                of(value.toQuantity(unit))

		@JvmStatic
		fun of(value: Number, unit: TemporalUnit): DsolTimeRef =
                of(value.toQuantity(unit))

		@JvmStatic
		fun of(value: Duration) = of(value.toQuantity())

		@JvmStatic
		fun of(value: Date, epoch: Date = Date.from(Instant.EPOCH)): DsolTimeRef =
                of(value.time - epoch.time, TimeRef.MILLISECOND)

		@JvmStatic
		fun of(value: Instant, epoch: Instant = Instant.EPOCH): DsolTimeRef =
                of(Duration.between(epoch, value))

	}
}
