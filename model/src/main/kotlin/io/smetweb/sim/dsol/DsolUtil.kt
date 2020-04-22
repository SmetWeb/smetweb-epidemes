package io.smetweb.sim.dsol

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import nl.tudelft.simulation.dsol.experiment.Experiment
import nl.tudelft.simulation.dsol.experiment.Replication
import nl.tudelft.simulation.dsol.experiment.Treatment
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterMap
import nl.tudelft.simulation.dsol.model.outputstatistics.OutputStatistic
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface.*
import nl.tudelft.simulation.event.EventInterface
import nl.tudelft.simulation.event.EventType
import nl.tudelft.simulation.event.TimedEvent
import tec.uom.se.ComparableQuantity
import java.math.BigDecimal
import javax.measure.quantity.Time

@Suppress("REDUNDANT_LABEL_WARNING")
fun DEVSSimulatorInterface<*, *, *>.emit(
		vararg eventTypes: EventType
): Observable<EventInterface> = Observable.create { emitter ->
	eventTypes.map { type -> DEVSSimulatorInterface@this.addListener(emitter::onNext, type) }
	DEVSSimulatorInterface@this.addListener(
			{ Schedulers.computation().scheduleDirect(emitter::onComplete) },
			END_REPLICATION_EVENT)
}

/**
 * @return a [DsolModel] [Replication] context (i.e. [Experiment]),
 * based on given some [analyst], some [description] and (required) treatment [config]
 */
fun DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>.createExperiment(
		config: DsolTreatment,
		analyst: String? = null,
		description: String? = null,
		inputParameters: InputParameterMap? = null,
		outputStatistics: MutableList<OutputStatistic<*>> = mutableListOf(),
		emitStatus: BehaviorSubject<EventType> = BehaviorSubject.create(),
		emitTime: BehaviorSubject<DsolTimeRef> = BehaviorSubject.create(),
		model: DsolModel = object: DsolModel {
			override val emitStatus = emitStatus
			override val emitTime = emitTime
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
			this, config.name, config.startTime, config.warmUpPeriod,
			config.runLength, config.replicationMode)
	this.simulator = simulator
	this.model = model
	val replication =  // FIXME fails on id value with negative #hashCode()
			Replication("r", this)
	this.replications = listOf(replication)

	// reset the scheduler's time and event list
	simulator.initialize(replication, this.treatment.replicationMode)

	// initial event to set the name of the simulator's worker thread
	simulator.scheduleEventAbs(DsolTimeRef.T_ZERO) {
		Thread.currentThread().name = this::class.java.simpleName + '@' + Integer.toHexString(this.hashCode())
	}

	// feed the observable simulator status stream
	simulator.emit(
			START_EVENT, STOP_EVENT, STEP_EVENT, WARMUP_EVENT, END_REPLICATION_EVENT, START_REPLICATION_EVENT)
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