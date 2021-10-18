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
 * TODO also provide [java.util.concurrent.Flow] reactive API from Java 9 ?
 */
interface ClockService {

	/** @return the common [Clock] instance, actual (e.g. [Clock.systemUTC]) or virtual */
	val now: Clock
		get() = Clock.systemUTC()

	/** @return the common clock's current time-zone [ZoneId] */
	fun zone(): ZoneId =
		now.zone

	/** @return the common clock's epoch as [Instant], with nanosecond precision */
	val epoch: Instant
		get() = Instant.EPOCH

	/** @return the common clock's epoch as [Date], with millisecond precision */
	val epochDate: Date
		get() = Date.from(this.epoch)

	/** @return the current time as [Instant], with nanosecond precision */
	fun instant(): Instant = now.instant()

	/** @return the current time as [Date], with millisecond precision */
	fun date(): Date = Date.from(instant())

	/** @return the current time as [TimeRef], with nanosecond precision*/
	fun time(): TimeRef = timeOf(instant())

	/** @return the common clock's [TimeRef] value for given [instant] */
	fun timeOf(instant: Instant): TimeRef = TimeRef.of(instant, this.epoch)

	/** @return the common clock's [TimeRef] value for given [date] */
	fun timeOf(date: Date): TimeRef = TimeRef.of(date, this.epochDate)

	/** @return the common clock's [TimeRef] value for given [delayMillis] from now */
	fun timeAfter(delayMillis: Long): TimeRef =
		timeAfter(delayMillis, TimeUnit.MILLISECONDS)

	/** @return the common clock's [TimeRef] value for given [unit]s of [delay] from now */
	fun timeAfter(delay: Number, unit: TimeUnit = TimeUnit.MILLISECONDS): TimeRef =
		timeAfter(delay.toQuantity(unit))

	/** @return the common clock's [TimeRef] value for given [TemporalUnit]s of [delay] from now */
	fun timeAfter(delay: Number, unit: TemporalUnit = ChronoUnit.MILLIS): TimeRef =
		timeAfter(delay.toQuantity(unit))

	/** @return the common clock's [TimeRef] value for given [Duration] of [delay] from now */
	fun timeAfter(delay: Duration): TimeRef =
		timeAfter(delay.toQuantity())

	/** @return the common clock's [TimeRef] value for given [Quantity] of [Time] [delay] from now */
	fun timeAfter(delay: Quantity<Time>): TimeRef =
		TimeRef.of(time().get().add(delay))

	/** @return the common clock's [Date] value for given [TimeUnit]s of [delay] from now */
	fun dateAfter(delay: Number, unit: TimeUnit): Date =
		timeAfter(delay, unit).toDate(this.epochDate)

	/** @return the [Quantity] of [Time] until the common clock reaches given [timeRef] (or the negative amount that has passed since) */
	fun durationUntil(timeRef: TimeRef): ComparableQuantity<Time> =
		timeRef.get().subtract(time().get())

	/** @return the [Quantity] of [Time] until the common clock reaches given [instant] (or the negative amount that has passed since) */
	fun durationUntil(instant: Instant): ComparableQuantity<Time> =
		durationUntil(timeOf(instant))

	/** @return the [Quantity] of [Time] until the common clock reaches given [date] (or the negative amount that has passed since) */
	fun durationUntil(date: Date): ComparableQuantity<Time> =
		durationUntil(timeOf(date))

	/** @return */
	fun trigger(
		event: (TimeRef) -> Unit,
		disposer: (Throwable?) -> Unit = { /* empty */ },
		firstTime: TimeRef,
		repeater: (TimeRef) -> TimeRef? = { null })

	/** call given [event] at [firstTime], and possibly recur as per [repeater]'s schedule (as of Java 1.8) */
	fun trigger(event: Consumer<TimeRef>, firstTime: TimeRef, repeater: UnaryOperator<TimeRef?>? = null) =
		trigger(event = event::accept, firstTime = firstTime, repeater = repeater?.let { it::apply } ?: { null })

	/** provides a new [Timer] scheduling API (as of Java 1.3) */
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