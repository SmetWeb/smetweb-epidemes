package io.smetweb.sim

import io.smetweb.time.TimeRef
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference

class SimClock(
		private val timeSupplier: () -> TimeRef,
		private val timeConverter: (TimeRef) -> Instant,
		private var zone: ZoneId = ZoneOffset.UTC
): Clock() {

	private val timeToInstantCache: AtomicReference<Pair<TimeRef, Instant>> = AtomicReference()

	override fun withZone(zone: ZoneId?): Clock {
		zone ?.let { this.zone = zone }
		return this
	}

	override fun getZone(): ZoneId = ZoneOffset.UTC

	override fun instant(): Instant = this.timeToInstantCache.updateAndGet { old ->
		val time = this.timeSupplier()
		if(old != null && old.first == time) {
			old
		} else {
			Pair(time, timeConverter(time))
		}
	}.second

}