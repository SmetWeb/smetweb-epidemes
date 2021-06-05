package io.smetweb.sim.dsol

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.smetweb.time.TimeRef
import io.smetweb.log.getLogger
import io.smetweb.sim.ScenarioConfig
import io.smetweb.time.ManagedClock
import io.smetweb.time.ManagedClockService
import io.smetweb.time.ManagedClockService.ClockStatus.*
import io.smetweb.time.ManagedRxTaskScheduler
import nl.tudelft.simulation.dsol.experiment.Experiment
import nl.tudelft.simulation.dsol.experiment.ReplicationInterface.*
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEventInterface
import nl.tudelft.simulation.dsol.simulators.DESSSimulatorInterface.*
import nl.tudelft.simulation.dsol.simulators.DEVSSimulator
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface
import tech.units.indriya.ComparableQuantity
import tech.units.indriya.unit.Units
import java.io.Serializable
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.measure.quantity.Time

/**
 * [DsolRxTaskScheduler] is a [ManagedRxTaskScheduler] that delegates to
 * a D-SOL [DEVSSimulatorInterface] which operates in [DsolTimeRef]-type time lines
 */
@Suppress("REDUNDANT_LABEL_WARNING")
class DsolRxTaskScheduler(
		private val scenarioConfig: ScenarioConfig,
		private val simSupplier: (id: Serializable) -> DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef> =
				{ id: Serializable -> DEVSSimulator(id) } , // default constructs a standard (i.e. single-threaded) simulator instance
		private val treatment: DsolTreatment = DsolTreatment() // default treatment is steady-state (i.e. non-terminating)
): ManagedRxTaskScheduler {

	private val log = getLogger()

	override val statusSource: BehaviorSubject<ManagedClockService.ClockStatus> = BehaviorSubject.create()

	override val timeSource: BehaviorSubject<TimeRef> = BehaviorSubject.create()

	override val epoch: Instant by lazy { this.scenarioConfig.epoch }

	override val epochDate: Date by lazy { Date.from(this.epoch) }

	/** *lazily* initialized [Experiment], which produces (emits) simulation and replication events */
	private val experiment: Experiment<ComparableQuantity<Time>, BigDecimal, DsolTimeRef,
			DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>> by lazy {
		val result = this.simSupplier(treatment.name).createExperiment(treatment = treatment,
				analyst = scenarioConfig.analyst, description = scenarioConfig.description)
		(result.model as DsolModel).timeSource.subscribe(this.timeSource)
		(result.model as DsolModel).statusSource.mapOptional { event ->
			when(event) {
				START_REPLICATION_EVENT -> Optional.of(INITIALIZING)
				START_EVENT -> Optional.of(STARTED)
				STOP_EVENT -> Optional.of(STOPPED)
				else -> Optional.empty()
			}
		}.subscribe(this.statusSource)
		result
	}

	private val simulator: DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>
		get() = this.experiment.simulator

	fun model(): DsolModel = this.experiment.model as DsolModel

	// ClockService

	private val dsolClock: Clock = ManagedClock(::time, { t -> t.toInstant(this.epoch) })

	override fun clock(): Clock = this.dsolClock

	override fun time(): DsolTimeRef = this.simulator.simTime ?: DsolTimeRef.T_ZERO

	override fun timeOf(date: Date): DsolTimeRef = DsolTimeRef.of(date, this.epochDate)

	override fun timeOf(instant: Instant): DsolTimeRef = DsolTimeRef.of(instant, this.epoch)

	// ManagedClockService

	override fun start() = this.simulator.start()

	override fun stop() = this.simulator.stop()

	override fun shutdown() {
		(this.simulator as DEVSSimulator).apply {
			if(this.isStartingOrRunning)
				stop()
			cleanUp()
		}
	}

	// RxClockService

	override fun trigger(schedule: Observable<TimeRef>): Observable<TimeRef> =
			// TODO map cancellable [SimEventInterface] through a [PublishSubject] rather than [AtomicReference]?
			Observable.create { emitter ->
				val nextEvent = AtomicReference<SimEventInterface<DsolTimeRef>>()
				schedule.subscribe( { triggerTime ->
					val now = time()
					val executionTime = maxOf(now, when(triggerTime) {
						is DsolTimeRef -> triggerTime
						else -> DsolTimeRef.of(triggerTime.get())
					})
					// check whether we are repeating (not initiating) at the current timeRef
					if(nextEvent.get() != null && executionTime == now) {
						log.warn("Potential infinite looping: {} -> {}", now, executionTime)
					}
					nextEvent.set(this.simulator.scheduleEventAbs(executionTime) {
						emitter.onNext(DsolTaskScheduler@time()) // actual (valid) time may differ from emitted one
					})
				}, emitter::onError, emitter::onComplete )
				emitter.setCancellable { nextEvent.get()?.let { this.simulator.cancelEvent(it) } }
			}

}