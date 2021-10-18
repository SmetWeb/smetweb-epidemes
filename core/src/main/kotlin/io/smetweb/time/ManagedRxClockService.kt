package io.smetweb.time

import io.reactivex.rxjava3.core.Observable
import io.smetweb.time.ManagedClockService.ClockStatus

/**
 * [ManagedRxClockService] extends [RxClockService] with [ManagedClockService] time controls
 */
@Suppress("UNUSED")
interface ManagedRxClockService: ManagedClockService, RxClockService {

	val statusSource: Observable<ClockStatus>

	val timeSource: Observable<TimeRef>

	override fun onStatusChanged(statusHandler: (ClockStatus) -> Unit, errorHandler: (Throwable) -> Unit) {
		statusSource.subscribe(statusHandler, errorHandler)
	}

	override fun onTimeChanged(timeHandler: (TimeRef) -> Unit, errorHandler: (Throwable) -> Unit) {
		timeSource.subscribe(timeHandler, errorHandler)
	}

}