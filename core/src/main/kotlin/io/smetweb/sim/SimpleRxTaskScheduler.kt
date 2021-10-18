package io.smetweb.sim

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.smetweb.log.getLogger
import io.smetweb.time.ManagedClockService
import io.smetweb.time.ManagedClockService.ClockStatus
import io.smetweb.time.ManagedRxTaskScheduler
import io.smetweb.time.TimeRef
import java.time.Instant
import java.time.ZoneId
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.measure.Quantity
import javax.measure.quantity.Time

class SimpleRxTaskScheduler(
	setupName: String = "rxSim",
	override val epoch: Instant = Instant.EPOCH,
	duration: Quantity<Time>,
	defaultZone: ZoneId = ZoneId.systemDefault()
): ManagedRxTaskScheduler {

	constructor(config: ScenarioConfig): this(
		setupName = config.setupName,
		epoch = config.epoch,
		duration = config.duration,
		defaultZone = config.defaultZone)

	private val log = getLogger()

	private val runName = setupName + "-" + Integer.toHexString(System.currentTimeMillis().hashCode())

	private val pending = TreeSet<TimeRef>()

	private val endRef: TimeRef = TimeRef.of(duration)

	private val worker = Schedulers.from(Executors.newSingleThreadExecutor())

	private var running = false

	override val statusSource: BehaviorSubject<ClockStatus> = BehaviorSubject.createDefault(ClockStatus.INITIALIZING)

	override val timeSource: BehaviorSubject<TimeRef> = BehaviorSubject.create()

	override val now = ManagedClockService.ManagedClock(::time, { it.toInstant(epoch) }, defaultZone)

	override fun time(): TimeRef =
		timeSource.value ?: TimeRef.T_ZERO

	override fun trigger(schedule: Observable<TimeRef>): Observable<TimeRef> =
		Observable.create { emitter ->
			val tNext = AtomicReference<TimeRef>()
			val disposable = schedule
				.flatMapSingle { t ->
					val tNow = time()
					if(t < tNow)
						Single.error(IllegalStateException("Can't schedule in past: $t < $tNow"))
					else {
						pending.add(t)
						Single.just(t)
					}
				}
				// store next scheduled timeRef for comparison in zipWith operation
				.doOnNext(tNext::set)
				// wait for scheduler to reach next scheduled timeRef
				.zipWith(timeSource.filter { now ->
					tNext.get()?.let { it == now } ?: false
				}) { _, now -> now }
				.subscribe(emitter::onNext, emitter::onError, emitter::onComplete)
			emitter.setCancellable(disposable::dispose)
		}

	override fun start() {
		worker.scheduleDirect {
			statusSource.value.let { state ->
				when(state) {
					ClockStatus.INITIALIZING -> {
						running = true
						initialize()
						resume()
						statusSource.onNext(ClockStatus.STARTED)
					}
					ClockStatus.STOPPED -> {
						running = true
						resume()
						statusSource.onNext(ClockStatus.STARTED)
					}
					else -> emitError("Can't start scheduler while status is $state")
				}
			}
		}
	}

	override fun stop() {
		worker.scheduleDirect {
			statusSource.value.let { state ->
				when (state) {
					ClockStatus.INITIALIZING -> {
						initialize()
						running = false
						statusSource.onNext(ClockStatus.STOPPED)
					}
					ClockStatus.STARTED -> {
						running = false
						statusSource.onNext(ClockStatus.STOPPED)
					}
					else -> emitError("Can't stop scheduler while status is $state")
				}
			}
		}
	}

	private fun emitError(text: String, cause: Throwable? = null) =
		IllegalStateException(text, cause).let {
			statusSource.onError(it)
			timeSource.onError(it)
		}

	private fun initialize() {
		Thread.currentThread().name = runName
		worker.scheduleDirect {
			timeSource.value.let { now ->
				when (now) {
					null -> {
						pending.add(TimeRef.T_ZERO)
						pending.add(endRef)
					}
					else -> emitError("Can't reset scheduler, currently at: $now")
				}
			}
		}
	}

	private fun resume() {
		worker.scheduleDirect {
			if(!running) {
				log.debug("Paused, upcoming t={}", if (pending.size == 0) "END" else pending.first())
			} else if(pending.isEmpty()) {
				log.debug("Completed: no more events pending")
				timeSource.onComplete()
				statusSource.onComplete()
			} else {
				val first = pending.first()
				timeSource.value?.let { now ->
					if(now > first)
						emitError("Can't proceed to past ($first), currently at: $now")
				}
				// remove first timeRef before upcoming simulation step (which may re-add it)
				if(!pending.remove(first))
					emitError("Unexpected: t=$first no longer pending?")

				// update time first and then emit, to perform simulation step
				timeSource.onNext(first)

				// schedule next step
				if(first != endRef)
					resume()
			}
		}
	}

}