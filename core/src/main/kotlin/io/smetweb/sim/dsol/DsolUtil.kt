package io.smetweb.sim.dsol

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import nl.tudelft.simulation.dsol.experiment.Experiment
import nl.tudelft.simulation.dsol.experiment.Replication
import nl.tudelft.simulation.dsol.experiment.Treatment
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterMap
import nl.tudelft.simulation.dsol.model.outputstatistics.OutputStatistic
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface.*
import org.djutils.event.EventInterface
import org.djutils.event.EventType
import org.djutils.event.TimedEvent
import tec.uom.se.ComparableQuantity
import java.math.BigDecimal
import javax.measure.quantity.Time

@Suppress("REDUNDANT_LABEL_WARNING")
fun DEVSSimulatorInterface<*, *, *>.emit(vararg eventTypes: EventType): Observable<EventInterface> =
		Observable.create { emitter ->
			eventTypes.map { type -> DEVSSimulatorInterface@this.addListener(emitter::onNext, type) }
			DEVSSimulatorInterface@this.addListener(
					{ Schedulers.computation().scheduleDirect(emitter::onComplete) },
					END_REPLICATION_EVENT)
		}

/**
 * @return a [DsolModel] [Replication] context (i.e. [Experiment]),
 * based on given some [analyst], some [description] and (required) treatment [treatment]
 */
fun DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>.createExperiment(
        treatment: DsolTreatment,
        analyst: String? = null,
        description: String? = null,
        inputParameters: InputParameterMap? = null,
        outputStatistics: MutableList<OutputStatistic<*>> = mutableListOf(),
        emitStatus: BehaviorSubject<EventType> = BehaviorSubject.create(),
        emitTime: BehaviorSubject<DsolTimeRef> = BehaviorSubject.create(),
        model: DsolModel = object: DsolModel {
			override val statusSource = emitStatus
			override val timeSource = emitTime
			override fun getSimulator() = simulator
			override fun getOutputStatistics(): MutableList<OutputStatistic<*>> = outputStatistics
			override fun getInputParameterMap() = inputParameters
			override fun constructModel() { /* no-op */ }
		},
        simulator: DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef> = this
) = Experiment<ComparableQuantity<Time>, BigDecimal, DsolTimeRef,
		DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>>().apply {
	this.analyst = analyst
	this.description = description
	this.treatment = Treatment<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>(
			this, treatment.name, treatment.startTime, treatment.warmUpPeriod,
			treatment.runLength, treatment.replicationMode)
	this.simulator = simulator
	this.model = model
	val replication =  // FIXME fails on id value with negative #hashCode()
			Replication("r", this)
	this.replications = listOf(replication)

	// reset the scheduler's time and event list
	simulator.initialize(replication, this.treatment.replicationMode)

	// initial event to set the name of the simulator's worker thread
	simulator.scheduleEventAbs(DsolTimeRef.T_ZERO) { Thread.currentThread().name = treatment.name }

	// feed the observable simulator status stream
	simulator.emit(START_EVENT, STOP_EVENT, STEP_EVENT, WARMUP_EVENT,
			END_REPLICATION_EVENT, START_REPLICATION_EVENT)
			.map(EventInterface::getType)
			.subscribe(emitStatus)

	// feed the observable simulation time stream
	simulator.emit(TIME_CHANGED_EVENT)
			.map { event ->
				@Suppress("UNCHECKED_CAST")
				(event as TimedEvent<ComparableQuantity<Time>>).timeStamp
			}
			.map(::DsolTimeRef)
			.subscribe(emitTime)
}