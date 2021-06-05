package io.smetweb.time

import io.reactivex.rxjava3.core.Observable

/**
 * [ManagedClockService] extends [ClockService] with time controls
 */
@Suppress("UNUSED")
interface ManagedClockService: ClockService {

	val statusSource: Observable<ClockStatus>

	val timeSource: Observable<TimeRef>

	fun start()

	fun stop()

	fun shutdown() { /* no-op */ }

	enum class ClockStatus {
		INITIALIZING,
		WARMED_UP,
		STARTED,
		STOPPED,
	}
}