package io.smetweb.time

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.toObservable
import io.smetweb.math.parseQuantity
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator
import org.joda.time.Period
import org.joda.time.format.ISOPeriodFormat
import org.quartz.CronScheduleBuilder
import org.quartz.CronTrigger
import org.quartz.TriggerBuilder
import tech.units.indriya.AbstractUnit
import java.time.*
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.measure.Quantity
import javax.measure.format.QuantityFormat
import javax.measure.quantity.Time

/**
 * [Timing] stores a [String] representation of some (calendar-based) recurrence pattern
 * and generates its respective time instant(s) as [TimeRef]s relative to some epoch or offset.
 *
 * Pattern syntax compatibility for [TimeRef] subtypes includes:
 *  - a [CRON expression][org.quartz.CronExpression], e.g.
 *    - `"0 0 0 14 * ? *"`, encoding: *midnight of every 14th day of the month*, or
 *    - `"0 30 9,12,15 * * ?"`, encoding: *every day at 9:30am, 12:30pm and 3:30pm*;
 *  - an iCal/RFC2445/RFC5545 [`RDATE`][org.dmfs.rfc5545.DateTime] or [`RRULE`][RecurrenceRuleIterator] pattern, e.g.:
 *    ```
 *    DTSTART;TZID=US-Eastern:19970902T090000
 *    RRULE:FREQ=DAILY;UNTIL=20130430T083000Z;INTERVAL=1;
 *    ```
 *  - a relative ISO8601 [`period` or `duration`][Duration] (e.g. `"P2DT3H4M"`); and
 *  - a relative JSR-275/363/385 [scientific][QuantityFormat] (e.g. units `"3 "` or duration `"27.5 µs"` ).
 */
sealed interface Timing {

    val pattern: String

    /** the maximum number of iterations to generate (if possible) */
    val max: Long?
        get() = null

    /**
     * @param startTime the virtual (relative) start [TimeRef]
     * @param offsetUtc the absolute UTC epoch or offset [Instant]
     * @return an [Iterable] stream of [TimeRef]s following this [Timing] pattern calculated from given offset
     */
    fun iterate(startTime: TimeRef, offsetUtc: Instant = Instant.EPOCH): Iterable<TimeRef>

}

/**
 * Applies given [clock] to trigger this [Timing] pattern and emit respective [TimeRef]s; failure or completion
 */
fun Timing.iterateOn(clock: RxClockService): Observable<TimeRef> =
    iterate(clock.time(), clock.epoch).let { clock.trigger(it.toObservable()) }

/**
 * Applies given [clock] to trigger this [Timing] pattern and call
 * - given [listener] at respective [TimeRef]s; and/or
 * - given [closer] on failure or completion
 */
fun Timing.iterateOn(
    clock: ClockService,
    listener: (TimeRef, Timing) -> Unit,
    closer: (Throwable?) -> Unit = { /* empty */ }
) =
    iterate(clock.time(), clock.epoch).iterator().let { iter ->
        if (iter.hasNext())
            clock.trigger(event = { listener(it, this) }, disposer = closer, firstTime = iter.next()) {
                if (iter.hasNext())
                    iter.next()
                else
                    null
            }
    }


/**
 * @return a new [Timing] pattern from this [CharSequence], assuming it is formatted as either:
 *  - an (absolute) [CRON expression][org.quartz.CronExpression], e.g.
 *    - `"0 0 0 14 * ? *"`, encoding: *midnight of every 14th day of the month*, or
 *    - `"0 30 9,12,15 * * ?"`, encoding: *every day at 9:30am, 12:30pm and 3:30pm*;
 *  - an (absolute) iCal/RFC2445/RFC5545 [`RRULE`][RecurrenceRuleIterator] pattern, e.g.:
 *    ```
 *    DTSTART;TZID=US-Eastern:19970902T090000
 *    RRULE:FREQ=DAILY;UNTIL=20130430T083000Z;INTERVAL=1;
 *    ```
 *  - a (relative) ISO8601 [`period` or `duration`][Duration] (e.g. `"P2DT3H4M"`); or
 *  - a (relative) JSR-275/363/385 [scientific][QuantityFormat] (e.g. duration `"27.5 µs"` or (default) time units `"3 "` ).
 */
fun CharSequence.parseTiming(
    max: Number? = null,
    defaultUnit: javax.measure.Unit<Time> = TimeRef.BASE_UNIT
): Timing {
    val maxLong = max?.toLong()
    val pattern = toString()
    return try {
        CronTiming(pattern, maxLong)
    } catch (cronErr: Exception) {
        try {
            ICalTiming(pattern, maxLong)
        } catch (rfcErr: Exception) {
            try {
                IsoTiming(pattern, maxLong)
            } catch (isoErr: Exception) {
                try {
                    QuantityTiming(pattern, maxLong, defaultUnit)
                } catch (jsrErr: Exception) {
                    error("""Problem parsing `$pattern` as either
        Quartz/cron expr: ${cronErr.message} (${cronErr.javaClass.simpleName})
      iCal/RFC-2445/5545: ${rfcErr.message} (${rfcErr.javaClass.simpleName}),
       Duration/ISO-8601: ${isoErr.message} (${isoErr.javaClass.simpleName}) or
         JSR-275/363/385: ${jsrErr.message} (${jsrErr.javaClass.simpleName})""")
                }
            }
        }
    }
}

class CronTiming(
    override val pattern: String,
    override val max: Long? = null,
    // fail fast
    val schedule: CronScheduleBuilder = CronScheduleBuilder.cronSchedule(pattern)
) : Timing {
    override fun iterate(startTime: TimeRef, offsetUtc: Instant) = Iterable {
        object : Iterator<TimeRef> {
            private val startSeconds = startTime.get().to(SECOND) // prevent endless rounding
            private val offsetMillis: Date = Date.from(offsetUtc)
            private val trigger: CronTrigger = TriggerBuilder.newTrigger()
                .startAt(offsetMillis)
                .withSchedule(schedule)
                .build()
            // just 1ms earlier, to make offset inclusive
            private var current: Date? = trigger.getFireTimeAfter(Date(offsetMillis.time - 1))
            private var count: Long = 0

            override fun hasNext(): Boolean =
                current != null && (max == null || count < max)

            override fun next(): TimeRef {
                val next: Date = current!!
                current = trigger.getFireTimeAfter(current)
                count++
                val interval = (next.time - offsetMillis.time).toQuantity(MILLISECOND)
                return TimeRef.of(startSeconds.add(interval))
            }
        }
    }
}

/**
 * [ICalTiming] handles iCal/RFC-2445/5545
 * [`RDATE`s, `EXDATE`s, `RRULE`s or `EXRULE`s][RecurrenceRule], e.g.
 *
 * ```
 * DTSTART;TZID=US-Eastern:19970902T090000
 * RRULE:FREQ=DAILY;UNTIL=20130430T083000Z;INTERVAL=1;
 * ```
 */
class ICalTiming(
    override val pattern: String,
    override val max: Long? = null,
    // fail fast
    val rule: RecurrenceRule = RecurrenceRule(pattern),
    val epoch: Instant = Instant.EPOCH,
    val zone: TimeZone = TimeZone.getDefault(),
) : Timing {
    override fun iterate(startTime: TimeRef, offsetUtc: Instant) = Iterable {
        object : Iterator<TimeRef> {
            private val iter = rule.iterator(startTime.toDate(Date.from(epoch)).time, zone)
            private val offsetMillis: Date = Date.from(offsetUtc)
            private var count: Long = 0

            override fun hasNext(): Boolean =
                iter.hasNext() && (max == null || count < max)

            override fun next(): TimeRef {
                count++
                val next = iter.nextMillis()
                val interval = (next - offsetMillis.time).toQuantity(MILLISECOND)
                return TimeRef.of(startTime.get().add(interval))
            }
        }
    }
}

/**
 * [IsoTiming] for [ISO-8601][ISOPeriodFormat] pattern formats
 */
class IsoTiming(
    override val pattern: String,
    override val max: Long? = null,
    private var jodaPeriod: AtomicReference<Period> = AtomicReference(),
    // fail fast
    private val intervalQuantity: Duration? = try {
        Duration.parse(pattern)
    } catch (durError: Exception) {
        jodaPeriod.set(JodaUtil.parsePeriod(pattern))
        null // interval deferred, depends on startTime and offsetUtc due to daylight savings, etc.
    },
) : Timing {
    override fun iterate(startTime: TimeRef, offsetUtc: Instant) = Iterable {
        object : Iterator<TimeRef> {
            val interval: Quantity<Time> = intervalQuantity?.toMillis()?.toQuantity(MILLISECOND) ?: let {
                JodaUtil.toQuantity(jodaPeriod.get(), offsetUtc.toEpochMilli() + startTime.toDuration().toMillis())
            }
            private var count: Long = 0

            override fun hasNext(): Boolean =
                max == null || count < max

            override fun next(): TimeRef =
                TimeRef.of(startTime.get().add(interval.multiply(count++)))
        }
    }
}

/**
 * [QuantityTiming] for [JSR-275/363/385 scientific][QuantityFormat] pattern formats
 */
class QuantityTiming(
    override val pattern: String,
    override val max: Long? = null,
    defaultUnit: javax.measure.Unit<Time> = TimeRef.BASE_UNIT,
    // fail fast
    val interval: Quantity<Time> = // pattern.parseTimeQuantity(defaultUnit)
        pattern.parseQuantity().let {
            if(it.unit == AbstractUnit.ONE)
                it.value.toQuantity(defaultUnit)
            else
                it.asType(Time::class.java)
        }
) : Timing {
    override fun iterate(startTime: TimeRef, offsetUtc: Instant) = Iterable {
        object : Iterator<TimeRef> {
            private var count: Long = 0

            override fun hasNext(): Boolean =
                max == null || count < max

            override fun next(): TimeRef =
                TimeRef.of(startTime.get().add(interval.multiply(count++)))
        }
    }
}
