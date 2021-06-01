package io.smetweb.time

import tech.units.indriya.ComparableQuantity
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*
import java.util.concurrent.TimeUnit
import javax.measure.Quantity
import javax.measure.quantity.Time

interface ClockService {

	fun clock(): Clock = Clock.systemUTC()

	val zone: ZoneId
		get() = clock().zone

	val epoch: Instant
		get() = Instant.EPOCH

	val epochDate: Date
		get() = Date.from(this.epoch)

	fun instant(): Instant = clock().instant()

	fun date(): Date = Date.from(instant())

	fun time(): TimeRef = timeOf(instant())

	fun timeOf(instant: Instant): TimeRef = TimeRef.of(instant, this.epoch)

	fun timeOf(date: Date): TimeRef = TimeRef.of(date, this.epochDate)

	fun timeAfter(delayMillis: Long): TimeRef =
			timeAfter(delayMillis, TimeUnit.MILLISECONDS)

	fun timeAfter(delay: Number, unit: TimeUnit = TimeUnit.MILLISECONDS): TimeRef =
			timeAfter(delay.toQuantity(unit))

	fun timeAfter(delay: Number, unit: TemporalUnit = ChronoUnit.MILLIS): TimeRef =
			timeAfter(delay.toQuantity(unit))

	fun timeAfter(duration: Duration): TimeRef =
			timeAfter(duration.toQuantity())

	fun timeAfter(delay: Quantity<Time>): TimeRef =
			TimeRef.of(time().get().add(delay))

	fun durationUntil(timeRef: TimeRef): ComparableQuantity<Time> =
			timeRef.get().subtract(time().get())

	fun durationUntil(instant: Instant): ComparableQuantity<Time> =
			durationUntil(timeOf(instant))

	fun durationUntil(date: Date): ComparableQuantity<Time> =
			durationUntil(timeOf(date))
}