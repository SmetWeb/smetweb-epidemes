package io.smetweb.time

import java.util.*
import java.util.function.Consumer
import java.util.function.UnaryOperator

/**
 * [ManagedClockService] extends [ClockService] with time controls and scheduled callback triggering,
 * including a [ManagedTimer] (modifying Java 1.3 [Timer] API) and compatibility with
 * Java 1.8 functional APIs [Consumer] and [UnaryOperator]
 *
 * TODO also provide `java.util.Flow` reactive API from Java 9 ?
 */
@Suppress("UNUSED")
interface ManagedClockService: ClockService {

	fun start()

	fun stop()

	fun shutdown() { /* no-op */ }

	fun trigger(listener: (TimeRef) -> Unit,
				disposer: (Throwable?) -> Unit = {},
				firstTime: TimeRef,
				repeater: (TimeRef) -> TimeRef? = { null })

	/** triggering callback(s) with functional APIs [Consumer] and [UnaryOperator] (as of Java 1.8) */
	fun trigger(listener: Consumer<TimeRef>, firstTime: TimeRef, repeater: UnaryOperator<TimeRef?>) =
			trigger(listener = listener::accept, firstTime = firstTime) { subsequentTime ->
				repeater.apply(subsequentTime)
			}

	/** triggering callback(s) with functional APIs [Consumer] and [UnaryOperator] (as of Java 1.8) */
	fun trigger(listener: Consumer<TimeRef>, repeater: UnaryOperator<TimeRef?>) =
			// first calculate initial time ref
			repeater.apply(time())?.let { firstTime ->
				// then schedule repeats
				trigger(listener = listener, firstTime = firstTime, repeater = repeater)
			}

	/** provides the [Timer] scheduling API (as of Java 1.3) */
	fun timer(): Timer = ManagedTimer(this)

	/** [ManagedTimer] provides the [Timer] scheduling API (as of Java 1.3) triggered by a managed [ManagedClockService] */
	open class ManagedTimer(
			private val managedClockService: ManagedClockService
	): Timer() {

		override fun schedule(task: TimerTask, delayMillis: Long) {
			val due = this.managedClockService.timeAfter(delayMillis)
			this.managedClockService.trigger( { task.run() }, { task.cancel() }, due)
		}

		override fun schedule(task: TimerTask, time: Date) {
			val due = this.managedClockService.timeOf(time)
			this.managedClockService.trigger({ task.run() }, { task.cancel() }, due)
		}

		override fun schedule(task: TimerTask, delayMillis: Long, periodMillis: Long) =
				scheduleAtFixedRate(task, delayMillis, periodMillis)

		override fun schedule(task: TimerTask, firstTime: Date, periodMillis: Long) =
				scheduleAtFixedRate(task, firstTime, periodMillis)

		override fun scheduleAtFixedRate(task: TimerTask, delayMillis: Long, periodMillis: Long) {
			val due = this.managedClockService.timeAfter(delayMillis)
			this.managedClockService.trigger({ task.run() }, { task.cancel() }, due) {
						this.managedClockService.timeAfter(periodMillis)
					}
		}

		override fun scheduleAtFixedRate(task: TimerTask, firstTime: Date, periodMillis: Long) {
			val due = this.managedClockService.timeOf(firstTime)
			this.managedClockService.trigger({ task.run() }, { task.cancel() }, due) {
						this.managedClockService.timeAfter(periodMillis)
					}
		}

		override fun purge(): Int = -1
	}
}