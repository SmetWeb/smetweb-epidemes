package io.smetweb.time

import io.smetweb.time.RxManagedClockService.RxScheduledFuture
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.Trigger
import org.springframework.scheduling.TriggerContext
import org.springframework.scheduling.annotation.Scheduled
import java.util.Date

/**
 * [ManagedTaskScheduler] extends [RxManagedClockService] to comply with
 * Spring's [TaskScheduler] API for scheduled task execution, and is therefore compatible with
 * Spring's [Trigger] API (including Spring's @[Scheduled] annotation accepting e.g.
 * [cron expressions](https://www.baeldung.com/cron-expressions))
 */
@Suppress("REDUNDANT_LABEL_WARNING", "UNUSED")
interface ManagedTaskScheduler: RxManagedClockService, TaskScheduler {

	fun Trigger.nextExecutionTime(): Date? =
			Trigger@this.nextExecutionTime(ManagedTriggerContext(SchedulerService@ date()))

	override fun schedule(task: Runnable, startTime: Date): RxScheduledFuture<Unit> =
			RxScheduledFuture(
					schedulerService = RxSchedulerService@this,
					command = task::run,
					startTime = startTime) // no repeat, task should run once only

	override fun scheduleAtFixedRate(task: Runnable, startTime: Date, periodMillis: Long): RxScheduledFuture<Unit> =
			RxScheduledFuture(
					schedulerService = RxSchedulerService@this,
					command = task::run,
					startTime = startTime,
					repeater = { now -> Date(now.time + periodMillis) })

	override fun schedule(task: Runnable, trigger: Trigger): RxScheduledFuture<Unit> =
			RxScheduledFuture(
					schedulerService = RxSchedulerService@this,
					command = task::run,
					startTime = trigger.nextExecutionTime()!!,
					repeater = { trigger.nextExecutionTime() })

	override fun scheduleAtFixedRate(task: Runnable, periodMillis: Long): RxScheduledFuture<*> =
			scheduleAtFixedRate(task, date(), periodMillis)

	// start time = end time, sim events are 'instantaneous'
	override fun scheduleWithFixedDelay(task: Runnable, startTime: Date, delayMillis: Long): RxScheduledFuture<*> =
			scheduleAtFixedRate(task, startTime, delayMillis)

	// start time = end time, sim events are 'instantaneous'
	override fun scheduleWithFixedDelay(task: Runnable, delayMillis: Long): RxScheduledFuture<*> =
			scheduleAtFixedRate(task, delayMillis)

	/** [ManagedTriggerContext] is a simple [TriggerContext] wrapping a single [Date] */
	data class ManagedTriggerContext(
			val lastCompletionTime: Date? = null,
			val lastScheduledExecutionTime: Date? = lastCompletionTime,
			val lastActualExecutionTime: Date? = lastCompletionTime
	): TriggerContext {
		override fun lastCompletionTime(): Date? = this.lastCompletionTime
		override fun lastScheduledExecutionTime(): Date? = this.lastScheduledExecutionTime
		override fun lastActualExecutionTime(): Date? = this.lastActualExecutionTime
	}
}