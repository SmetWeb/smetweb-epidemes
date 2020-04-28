package io.smetweb.sim.dsol

import io.reactivex.rxjava3.core.Observable
import io.smetweb.time.TimeRef
import io.smetweb.log.getLogger
import io.smetweb.sim.ScenarioConfig
import io.smetweb.time.ManagedClock
import io.smetweb.time.ManagedTaskScheduler
import nl.tudelft.simulation.dsol.experiment.Experiment
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEventInterface
import nl.tudelft.simulation.dsol.simulators.DEVSSimulator
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface
import org.springframework.stereotype.Service
import tec.uom.se.ComparableQuantity
import java.io.Serializable
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import javax.measure.quantity.Time

/**
 * [DsolTaskScheduler] is a [ManagedTaskScheduler] and Spring [Service] that delegates to
 * a D-SOL [DEVSSimulatorInterface] which operates in [DsolTimeRef]-type time lines
 */
@Service
@Suppress("REDUNDANT_LABEL_WARNING")
class DsolTaskScheduler(
		private val scenarioConfig: ScenarioConfig,
		private val simSupplier: (id: Serializable) -> DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef> =
				{ id: Serializable -> DEVSSimulator(id) } , // default constructs a standard (i.e. single-threaded) simulator instance
		private val treatment: DsolTreatment = DsolTreatment() // default treatment is steady-state (i.e. non-terminating)
): ManagedTaskScheduler {

	private val log = getLogger()

	override val epoch: Instant by lazy { this.scenarioConfig.epoch }

	override val epochDate: Date by lazy { Date.from(this.epoch) }

	/** *lazily* initialized [Experiment], which produces (emits) simulation and replication events */
	private val experiment: Experiment<ComparableQuantity<Time>, BigDecimal, DsolTimeRef,
			DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>> by lazy {
		this.simSupplier(treatment.name).createExperiment(treatment = treatment,
				analyst = scenarioConfig.analyst, description = scenarioConfig.description)
	}

	private val simulator: DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>
		get() = this.experiment.simulator

	fun model(): DsolModel = this.experiment.model as DsolModel

	// ClockService

	private val dsolClock: Clock = ManagedClock(::time, { t -> t.toInstant(this.epoch) })

	override fun clock(): Clock = this.dsolClock

	override fun time(): DsolTimeRef = this.simulator.simTime ?: DsolTimeRef.T_ZERO

	override fun timeOf(date: Date): DsolTimeRef = DsolTimeRef.of(date, this.epochDate)

	override fun timeOf(instant: Instant): TimeRef = DsolTimeRef.of(instant, this.epoch)

	// SchedulerService

	override fun start() = this.simulator.start()

	override fun stop() = this.simulator.stop()

	override fun shutdown() {
		(this.simulator as DEVSSimulator).apply {
			stop()
			cleanUp()
		}
	}

	// RxSchedulerService

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