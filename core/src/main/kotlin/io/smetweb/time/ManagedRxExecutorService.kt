package io.smetweb.time

import io.smetweb.time.RxClockService.RxScheduledFuture
import java.util.Date
import java.util.concurrent.*
import javax.enterprise.concurrent.*

/**
 * [ManagedRxExecutorService] provides the [ManagedScheduledExecutorService] scheduling API
 * (as of Java 1.5) specified in the JSR-236 concurrency API for JVM and servlet containers
 */
@Suppress("REDUNDANT_LABEL_WARNING", "UNUSED")
interface ManagedRxExecutorService: ManagedRxClockService, ManagedScheduledExecutorService {

	fun Trigger.startDate(): Date =
			Trigger@this.getNextRunTime(null, SchedulerService@date())

	fun Trigger.toDateSupplier(): (Date) -> Date? =
			{ now: Date ->
				val lastExecution = ManagedLastExecution(time = now)
				if(Trigger@this.skipRun(lastExecution, now)) {
					throw SkippedException()
				}
				Trigger@this.getNextRunTime(lastExecution, now)
			}

	override fun schedule(command: Runnable, trigger: Trigger): RxScheduledFuture<Unit> =
			RxScheduledFuture(
					schedulerService = RxSchedulerService@ this,
					command = command::run,
					startTime = trigger.startDate(),
					repeater = trigger.toDateSupplier())

	override fun <V : Any?> schedule(callable: Callable<V>, trigger: Trigger): RxScheduledFuture<V> =
			RxScheduledFuture(
					schedulerService = RxSchedulerService@ this,
					command = callable::call,
					startTime = trigger.startDate(),
					repeater = trigger.toDateSupplier())

	override fun schedule(command: Runnable, delay: Long, unit: TimeUnit): RxScheduledFuture<Unit> =
			RxScheduledFuture(
					schedulerService = RxSchedulerService@ this,
					command = command::run,
					startTime = dateAfter(delay, unit))

	override fun <V : Any?> schedule(callable: Callable<V>, delay: Long, unit: TimeUnit): RxScheduledFuture<V> =
			RxScheduledFuture(
					schedulerService = RxSchedulerService@ this,
					command = callable::call,
					startTime = dateAfter(delay, unit))

	override fun scheduleAtFixedRate(command: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): RxScheduledFuture<Unit> =
			RxScheduledFuture(
					schedulerService = SchedulerService@ this,
					command = command::run,
					startTime = dateAfter(initialDelay, unit),
					repeater = { dateAfter(period, unit) })

	// start time = end time, sim events are 'instantaneous'
	override fun scheduleWithFixedDelay(command: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit): RxScheduledFuture<Unit> =
			scheduleAtFixedRate(command, initialDelay, delay, unit)

	override fun <T : Any?> submit(task: Callable<T>): Future<T> {
		throw UnsupportedOperationException("")
	}

	override fun <T : Any?> submit(task: Runnable, result: T): Future<T> {
		throw UnsupportedOperationException("")
	}

	override fun submit(task: Runnable): Future<*> {
		throw UnsupportedOperationException("")
	}

	override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
		throw UnsupportedOperationException("")
	}

	override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T {
		throw UnsupportedOperationException("")
	}

	override fun isTerminated(): Boolean {
		throw UnsupportedOperationException("")
	}

	override fun execute(command: Runnable) {
		throw UnsupportedOperationException("")
	}

	override fun shutdown() {
		throw UnsupportedOperationException("")
	}

	override fun shutdownNow(): MutableList<Runnable> {
		throw UnsupportedOperationException("")
	}

	override fun isShutdown(): Boolean {
		throw UnsupportedOperationException("")
	}

	override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
		throw UnsupportedOperationException("")
	}

	override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
		throw UnsupportedOperationException("")
	}

	override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): MutableList<Future<T>> {
		throw UnsupportedOperationException("")
	}

	/**
	 * [ManagedLastExecution] is a simple [LastExecution] wrapping a single [Date],
	 * with default empty [result] and [identityName]
	 */
	data class ManagedLastExecution(
			val time: Date,
			private val result: Any? = null,
			private val identityName: String? = null
	): LastExecution {
		override fun getScheduledStart(): Date = this.time
		override fun getRunStart(): Date = this.time
		override fun getRunEnd(): Date = this.time
		override fun getResult(): Any? = this.result
		override fun getIdentityName(): String? = this.identityName
	}
}