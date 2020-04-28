package io.smetweb.sim.dsol

import nl.tudelft.simulation.dsol.experiment.ReplicationMode
import java.math.BigDecimal

data class DsolTreatment(
        val name: String = "sim-" + System.currentTimeMillis(),
        val replicationMode: ReplicationMode = ReplicationMode.STEADY_STATE,
        val startTime: DsolTimeRef = DsolTimeRef.T_ZERO,
        val warmUpPeriod: BigDecimal = BigDecimal.ONE,
        val warmUpTime: DsolTimeRef = startTime.copy().apply { add(warmUpPeriod) },
        val runLength: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE),
        val endTime: DsolTimeRef? = startTime.copy().apply { add(runLength) }
)