package io.smetweb.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * [ManagedClockService] extends [ClockService] with time controls
 */
@Suppress("UNUSED")
interface ManagedClockService: ClockService {

	override val now: ManagedClock

	fun start()

	fun stop()

	fun shutdown() { /* no-op */ }

	fun onStatusChanged(statusHandler: (ClockStatus) -> Unit, errorHandler: (Throwable) -> Unit = Throwable::printStackTrace)

	fun onTimeChanged(timeHandler: (TimeRef) -> Unit, errorHandler: (Throwable) -> Unit = Throwable::printStackTrace)

	/**
	 * [ClockStatus] describes the current state of the [ManagedClock]
	 */
	enum class ClockStatus {
		INITIALIZING,
		STARTED,
		STOPPED,
	}

	/**
	 * A [ManagedClock] overrides Java's [Clock] API and re-routes time calculations onto a managed [timeRefSupplier],
	 * using [timeRefConverter] to obtain [Instant] and [Date] values based on a fixed offset, e.g. [ClockService.epoch]
	 */
	class ManagedClock(
		private val timeRefSupplier: () -> TimeRef,
		private val timeRefConverter: (TimeRef) -> Instant = { it.toInstant(Instant.EPOCH) },
		private var timeZone: ZoneId = ZoneOffset.UTC
	): Clock() {

		private val timeToInstantCache: AtomicReference<Pair<TimeRef, Instant>> = AtomicReference()

		override fun withZone(zone: ZoneId): Clock {
			this.timeZone = zone
			return this
		}

		override fun getZone(): ZoneId = this.timeZone

		override fun instant(): Instant = toInstant()

		// used by e.g. Spring's ScheduledTaskRegistrar.scheduleFixedDelayTask(..)
		override fun millis(): Long = Date.from(toInstant()).time

		private fun toInstant(): Instant =
			this.timeToInstantCache.updateAndGet { old ->
				val time = this.timeRefSupplier()
				if (old?.first == time) {
					old // unchanged timeRef, same instant
				} else {
					Pair(time, this.timeRefConverter(time))
				}
			}.second
	}

}