package io.smetweb.sim.dsol

import io.smetweb.math.NUMBER_SYSTEM
import io.smetweb.math.decimalValue
import io.smetweb.math.toUnit
import io.smetweb.sim.ScenarioConfig
import tech.units.indriya.function.Calculus
import java.math.BigDecimal

data class DsolTreatment(
    val name: String = "dsolSim-" + Integer.toHexString(System.currentTimeMillis().hashCode()),
    val startTime: DsolTimeRef = DsolTimeRef.T_ZERO,
    val warmUpPeriod: BigDecimal = BigDecimal.ZERO,
    val warmUpTime: DsolTimeRef = startTime.copy().apply { add(warmUpPeriod) },
    val runLength: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE),
    val endTime: DsolTimeRef = startTime.copy().apply { add(runLength) },
    val numberOfReplications: Int = 1
) {

    constructor(config: ScenarioConfig): this(
        name = config.setupName + "-" + Integer.toHexString(System.currentTimeMillis().hashCode()),
        runLength = config.duration.toUnit(DsolTimeRef.BASE_UNIT).decimalValue()
    )

    init {
        // TODO move elsewhere?
        Calculus.setCurrentNumberSystem(NUMBER_SYSTEM)
    }

}