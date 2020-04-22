package io.smetweb.time

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import javax.measure.Quantity
import javax.measure.quantity.Time

/**
 * [RxSchedulerService] extends [SchedulerService] with RxJava API (Java 8)
 */
@Suppress("REDUNDANT_LABEL_WARNING", "UNUSED")
interface RxSchedulerService: SchedulerService {

	fun trigger(schedule: Observable<TimeRef>): Observable<TimeRef>

	fun trigger(firstTime: TimeRef, repeater: (TimeRef) -> TimeRef? = { null }): Observable<TimeRef> {
		val schedule = PublishSubject.create<TimeRef>()
		return trigger(schedule = Observable.just(firstTime)
				.concatWith(schedule))
				.doOnNext { time ->
					try {
						repeater(time)?.let(schedule::onNext) ?: schedule.onComplete()
					} catch (e: Throwable) {
						schedule.onError(e)
					}
				}
	}

	override fun trigger(
			listener: (TimeRef) -> Unit,
			disposer: (Throwable?) -> Unit,
			firstTime: TimeRef,
			repeater: (TimeRef) -> TimeRef?
	) {
		trigger(firstTime, repeater).subscribe(listener, disposer, { disposer(null) })
	}

	/** @see [Observable.timer] */
	fun timer(
			delay: Number,
			unit: TimeUnit = TimeUnit.MILLISECONDS
	): Single<TimeRef> = trigger(timeAfter(delay, unit)).firstOrError()

	/** @see [Observable.interval] */
	fun interval(
			initialDelay: Number? = null,
			period: Number,
			unit: TimeUnit = TimeUnit.MILLISECONDS,
			interval: Quantity<Time> = period.toQuantity(unit)
	): Observable<TimeRef> = trigger(timeAfter(initialDelay ?: 0L, unit)) {
		timeAfter(interval) // equivalent to `it -> TimeRef.of(it.get().add(interval))`
	}

	/** @see [Observable.delay] operator */
	fun <V: Any> Observable<V>.delayManaged(
			delay: Number,
			unit: TimeUnit = TimeUnit.MILLISECONDS,
			scheduler: RxSchedulerService = this@RxSchedulerService
	): Observable<V> = Observable@this.flatMapSingle { value ->
		scheduler.timer(delay, unit).map { value }
	}

	/**
	 * [RxScheduledFuture] decorator wraps a [CompletableFuture]
	 * that is linked to its [Observable] event [schedule]
	 * which emits the expected execution [Date] of the [command],
	 * just *after* the current execution has finished
	 * and *after* the consecutive execution (if any) has been scheduled
	 *
	 * TODO apply generated schedule directly to [RxSchedulerService.trigger] API
	 * TODO implement a [ScheduledFuture] without any RxJava components for [SchedulerService]
	 */
	@Suppress("MemberVisibilityCanBePrivate")
	class RxScheduledFuture<V: Any?>(
			private val schedulerService: RxSchedulerService,
			private val future: CompletableFuture<V> = CompletableFuture(), // the wrapped [Future]
			private val result: AtomicReference<V> = AtomicReference(),
			private val command: () -> V?,
			startTime: Date = schedulerService.date(), // immediately, i.e. as soon as possible
			repeater: (Date) -> Date? = { null }, // default: no rescheduling
			val schedule: Observable<Date> = Observable.create { emitter ->
				emitter.onNext(startTime)  // always emit startTime
				// schedule emission of next trigger event
				val startTimeRef = schedulerService.timeOf(startTime)
				schedulerService.trigger(startTimeRef) trigger@{
					val time = schedulerService.date()
					// schedule next trigger event
					return@trigger try {
						repeater(time)?.let { next ->
							emitter.onNext(next) // emit next schedule time-value, for delay comparison
							schedulerService.timeOf(next)
						}
					} catch (e: Throwable) {
						emitter.onError(e) // emit failure that occurred in the repeater
						null
					}
				} // TODO .doAfterNext( /* trigger execution *after* all subscribers handled the 'next execution time' */)
						.subscribe({}, // non-null result completes the future, else continue
								emitter::onError, // simulator failed
								emitter::onComplete) // trigger or simulation completed
			}
	): ScheduledFuture<V> {

		private val nextExecutionTime: AtomicReference<Date> = AtomicReference()

		private val subscription: Disposable

		init {
			// add command executions *once* as first subscription
			this.subscription = this.schedule.subscribe(
					{ nextTime: Date? ->
						// update next execution time
						this.nextExecutionTime.set(nextTime)
						// trigger execution and store result
						this.command()?.let(this.result::set)
					},
					{ this.future.completeExceptionally(it) },
					{ this.future.complete(this.result.get()) })
		}

		override fun get(): V = this.future.get()

		override fun get(timeout: Long, unit: TimeUnit): V = this.future.get(timeout, unit)

		override fun getDelay(unit: TimeUnit): Long =
				this.schedulerService.durationUntil(pendingExecutionTime()) // relative to current (virtual) time
						.longValue(unit) // convert to specified time unit and truncate to 64-bit (long) value

		private fun pendingExecutionTime(): Date = this.nextExecutionTime.get()!!

		override fun compareTo(other: Delayed): Int =
				this.pendingExecutionTime().compareTo((other as RxScheduledFuture<*>).pendingExecutionTime())

		override fun isCancelled(): Boolean = this.future.isCancelled

		override fun isDone(): Boolean = this.future.isDone

		override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
			this.subscription.dispose() // should unsubscribe listeners and cancel scheduled simulation event
			return !this.future.isDone && this.future.cancel(mayInterruptIfRunning)
		}

	}
}