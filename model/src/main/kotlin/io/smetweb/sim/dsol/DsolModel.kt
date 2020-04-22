package io.smetweb.sim.dsol

import io.reactivex.Observable
import nl.tudelft.simulation.dsol.model.DSOLModel
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface
import nl.tudelft.simulation.event.EventType
import tec.uom.se.ComparableQuantity
import java.math.BigDecimal
import javax.measure.quantity.Time

interface DsolModel: DSOLModel<ComparableQuantity<Time>, BigDecimal, DsolTimeRef,
		DEVSSimulatorInterface<ComparableQuantity<Time>, BigDecimal, DsolTimeRef>> {

	val emitStatus: Observable<EventType>

	val emitTime: Observable<DsolTimeRef>

}