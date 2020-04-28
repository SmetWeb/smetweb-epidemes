package io.smetweb.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference

class ManagedClock(
		private val timeRefSupplier: () -> TimeRef,
		private val timeRefConverter: (TimeRef) -> Instant = { it.toInstant(Instant.EPOCH) },
		private var timeZone: ZoneId = ZoneOffset.UTC
): Clock() {

	private val timeToInstantCache: AtomicReference<Pair<TimeRef, Instant>> = AtomicReference()

	override fun withZone(zone: ZoneId?): Clock {
		zone ?.let { this.timeZone = zone }
		return this
	}

	override fun getZone(): ZoneId = this.timeZone

	override fun instant(): Instant = this.timeToInstantCache.updateAndGet { old ->
		val time = this.timeRefSupplier()
		if(old?.first == time) {
			old // unchanged timeRef, same instant
		} else {
			Pair(time, this.timeRefConverter(time))
		}
	}.second

}