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
import java.util.function.Consumer
import java.util.function.UnaryOperator
import javax.measure.Quantity
import javax.measure.quantity.Time

/**
 * [ClockService] keeps a [Clock] for providing a common [TimeRef], including conversion methods
 * for `java.time` ([Duration], [Instant], [ChronoUnit] and [TemporalUnit]),
 * `java.util.concurrent` [TimeUnit], and JSR-275/363/385 `javax.measure` [Time].
 *
 * [ClockService] also provides scheduled callback triggering,
 * including a [ClockTimer] (extending the Java 1.3 [Timer] API),
 * but also supporting Java 1.8 functional APIs [Consumer] and [UnaryOperator]
 *
 * TODO also provide `java.util.Flow` reactive API from Java 9 ?
 */
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

	fun dateAfter(delay: Number, unit: TimeUnit): Date =
		timeAfter(delay, unit).toDate(this.epochDate)

	fun durationUntil(timeRef: TimeRef): ComparableQuantity<Time> =
		timeRef.get().subtract(time().get())

	fun durationUntil(instant: Instant): ComparableQuantity<Time> =
		durationUntil(timeOf(instant))

	fun durationUntil(date: Date): ComparableQuantity<Time> =
		durationUntil(timeOf(date))

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
	fun timer(): Timer = ClockTimer(this)

	/** [ClockTimer] provides the [Timer] scheduling API (as of Java 1.3) triggered by a [ClockService] */
	open class ClockTimer(private val clockService: ClockService): Timer() {

		override fun schedule(task: TimerTask, delayMillis: Long) {
			val due = this.clockService.timeAfter(delayMillis)
			this.clockService.trigger( { task.run() }, { task.cancel() }, due)
		}

		override fun schedule(task: TimerTask, time: Date) {
			val due = this.clockService.timeOf(time)
			this.clockService.trigger({ task.run() }, { task.cancel() }, due)
		}

		override fun schedule(task: TimerTask, delayMillis: Long, periodMillis: Long) =
			scheduleAtFixedRate(task, delayMillis, periodMillis)

		override fun schedule(task: TimerTask, firstTime: Date, periodMillis: Long) =
			scheduleAtFixedRate(task, firstTime, periodMillis)

		override fun scheduleAtFixedRate(task: TimerTask, delayMillis: Long, periodMillis: Long) {
			val due = this.clockService.timeAfter(delayMillis)
			this.clockService.trigger({ task.run() }, { task.cancel() }, due) {
				this.clockService.timeAfter(periodMillis)
			}
		}

		override fun scheduleAtFixedRate(task: TimerTask, firstTime: Date, periodMillis: Long) {
			val due = this.clockService.timeOf(firstTime)
			this.clockService.trigger({ task.run() }, { task.cancel() }, due) {
				this.clockService.timeAfter(periodMillis)
			}
		}

		override fun purge(): Int = -1
	}
}