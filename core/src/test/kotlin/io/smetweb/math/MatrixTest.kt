package io.smetweb.math

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.ujmp.core.Matrix
import org.ujmp.core.enums.ValueType
import java.math.BigDecimal

class MatrixTest {

    @Test
    fun `test summation`() {
        val m =
            buildSparseMatrix {
                valueType = ValueType.BIGDECIMAL
                size = 5 by 2000
            }
            .put(1,0,0)
            .put(2,1,0)
            .put(3,0,1)
            .put(4,1,1)
            .put(5,0,2)
            .put(6,1,2)
        val sum = 21L
        println(m::class.java.name + ":\n" + m)
        assertEquals(sum.toDecimal(), m.sum())

        m.add(BigDecimal.ONE)
        m.forEach { x, v ->
            if(x[0] > 1 && x[1] > 2) {
                assertEquals(BigDecimal.ONE, v, "$v should be 1")
            }
        }
        val total = sum + m.rowCount * m.columnCount
        assertEquals(total.toDecimal(), m.sum(), "total calculation failed")
    }

    /** the epidemiological compartments */
    enum class EpiPart {
        /** susceptible  */
        S,

        /** infective  */
        I,

        /** recovered  */
        R
    }

    @Test
    fun testSIRIntegrationForwardEuler() {

        // basic reproduction ratio (dimensionless) = beta/gamma
        val recovery = BigDecimal.valueOf(12)
        val dt = BigDecimal.valueOf(.1)
        val reproductionRate = BigDecimal.valueOf(14)

        val dtRate: BigDecimal = dt.inverse()
        val gamma: BigDecimal = recovery.inverse()
        val beta = gamma.multiply(reproductionRate)
        val endTime = 100L

        // some matrix indices
        val compartmentCount = EpiPart.values().size.toLong()
        val rowSusceptibles = EpiPart.S.ordinal.toLong()
        val rowInfectives = EpiPart.I.ordinal.toLong()
        val rowRemoved = EpiPart.R.ordinal.toLong()
        val rateCount = compartmentCount + 2
        val rowSI = rateCount - 2
        val rowN = rateCount - 1

        // initialize population structure
        val rates =
            buildSparseMatrix {
                size = rateCount by compartmentCount
                label = "Rates"
                label(rowSusceptibles, "S", "dS")
                label(rowInfectives, "I", "dI")
                label(rowRemoved, "R", "dR")
                label(rowSI, "SI/N")
                label(rowN, "N")
            }
            // infection: beta * SI/N, flows from S to I
            .subtract(beta, rowSI, rowSusceptibles)
            .add(beta, rowSI, rowInfectives)
            // recovery: gamma * I, flows from I to R
            .subtract(gamma, rowInfectives, rowInfectives)
            .add(gamma, rowInfectives, rowRemoved)

        // row-vector of population's compartments
        val population: Matrix =
            buildSparseMatrix {
                size = 1 by compartmentCount
                label = "Population"
            }
            .put(mapOf(EpiPart.S to 999, EpiPart.I to 1, EpiPart.R to 0), 0, 0)

        // row-vector for updating SIR's ordinal differential equation (ODE) terms
        val terms: Matrix =
            buildSparseMatrix {
                size = 1 by rateCount
                label = "Terms"
                label(rowSI, "SI/N")
                label(rowN, "N")
                labelColumns(*EpiPart.values())
            }

        val total: BigDecimal = population.sum()

        println("reproduction rate (R_0): $reproductionRate, recovery period: $recovery, beta: $beta, gamma: $gamma, dt: $dtRate, population (N): $total, M_rate:\n$rates")

        val results: Matrix =
            buildSparseMatrix {
                valueType = ValueType.BIGDECIMAL
                size = endTime by 2 * compartmentCount
                label = "Forw.Euler"
                rowLabeler = { i -> "t=" + dt.multiply(BigDecimal(i)) }
                columnLabeler = { i -> ((if (i < compartmentCount) "" else "d") + EpiPart.values()[(i % compartmentCount).toInt()]) }
            }
            .put(population, 0, 0)

        for (t in 1 until endTime) {

            val newSIoverN = population.get(0, rowSusceptibles).multiply(population.get(0, rowInfectives)).divide(total)

            // update terms
            terms.put(population).put(newSIoverN, 0, rowSI).put(total, 0, rowN)

            // calculate deltas
            val deltas: Matrix = terms.mtimes(rates).multiply(dt)

            // store results
            results.put(deltas, t - 1, compartmentCount)
            results.put(population.add(deltas), t, 0)
        }
        println("Results: \n$results")
    }
}