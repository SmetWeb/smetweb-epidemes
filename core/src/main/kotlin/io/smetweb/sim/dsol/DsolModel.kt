package io.smetweb.sim.dsol

import io.reactivex.rxjava3.core.Observable
import nl.tudelft.simulation.dsol.model.DSOLModel
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface
import org.djutils.event.EventTypeInterface
import tech.units.indriya.ComparableQuantity
import java.math.BigDecimal
import javax.measure.quantity.Time

interface DsolModel: DSOLModel<ComparableQuantity<Time>, BigDecimal, DsolTimeRef,
		DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>> {

	val statusSource: Observable<EventTypeInterface>

	val timeSource: Observable<DsolTimeRef>

}