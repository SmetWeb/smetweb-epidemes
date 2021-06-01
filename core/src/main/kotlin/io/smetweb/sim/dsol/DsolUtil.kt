package io.smetweb.sim.dsol

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import nl.tudelft.simulation.dsol.experiment.*
import nl.tudelft.simulation.dsol.experiment.ReplicationInterface.*
import nl.tudelft.simulation.dsol.model.inputparameters.InputParameterMap
import nl.tudelft.simulation.dsol.simulators.DESSSimulatorInterface.*
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface
import nl.tudelft.simulation.dsol.statistics.StatisticsInterface
import org.djutils.event.EventInterface
import org.djutils.event.EventTypeInterface
import org.djutils.event.TimedEvent
import tech.units.indriya.ComparableQuantity
import java.math.BigDecimal
import javax.measure.quantity.Time

@Suppress("REDUNDANT_LABEL_WARNING")
fun DEVSSimulatorInterface<*, *, *>.emit(vararg eventTypes: EventTypeInterface): Observable<EventInterface> =
		Observable.create { emitter ->
			eventTypes.map { type -> DEVSSimulatorInterface@this.addListener(emitter::onNext, type) }
			DEVSSimulatorInterface@this.addListener(
					{ Schedulers.computation().scheduleDirect(emitter::onComplete) },
					END_REPLICATION_EVENT)
		}

fun DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>.createEmptyModel(
	emitStatus: BehaviorSubject<EventTypeInterface> = BehaviorSubject.create(),
	emitTime: BehaviorSubject<DsolTimeRef> = BehaviorSubject.create(),
	inputParameters: InputParameterMap? = null,
	outputStatistics: MutableList<StatisticsInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>> = mutableListOf(),
	simulator: DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef> = this
): DsolModel = object: DsolModel {
	private var streamInformation: StreamInformation = StreamInformation() // wrap external PRNG as DSOL stream?
	override val statusSource = emitStatus
	override val timeSource = emitTime
	override fun constructModel() { /* no-op */ } // trigger model reset event?
	override fun getSimulator() = simulator
	override fun getInputParameterMap() = inputParameters
	override fun getOutputStatistics(): MutableList<StatisticsInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>> = outputStatistics
	override fun getStreamInformation(): StreamInformation = this.streamInformation
	override fun setStreamInformation(streamInformation: StreamInformation) {
		this.streamInformation = streamInformation
	}
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
	outputStatistics: MutableList<StatisticsInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>> = mutableListOf(),
	emitStatus: BehaviorSubject<EventTypeInterface> = BehaviorSubject.create(),
	emitTime: BehaviorSubject<DsolTimeRef> = BehaviorSubject.create(),
	simulator: DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef> = this,
	model: DsolModel = createEmptyModel(emitStatus, emitTime, inputParameters, outputStatistics, simulator),
	replication: ReplicationInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef> = SingleReplication(treatment.name, treatment.startTime, treatment.warmUpPeriod, treatment.runLength)
) = Experiment(
	simulator,
	model,
	ExperimentRunControl(treatment.name, treatment.startTime, treatment.warmUpPeriod, treatment.runLength, treatment.numberOfReplications)).apply {
	this.description = description

	// reset the scheduler's time and event list
	simulator.initialize(model, replication)

	// initial event to set the name of the simulator's worker thread
	simulator.scheduleEventAbs(DsolTimeRef.T_ZERO) { Thread.currentThread().name = treatment.name }

	// feed the observable simulator status stream
	simulator.emit(START_EVENT, STOP_EVENT, TIME_STEP_CHANGED_EVENT, WARMUP_EVENT,
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