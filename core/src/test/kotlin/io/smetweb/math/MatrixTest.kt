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
            sparseMatrix {
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
        println(m.javaClass.name + ":\n" + m)
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
        val dt_ratio: BigDecimal = dt.inverse()
        val R_0 = BigDecimal.valueOf(14)
        val gamma: BigDecimal = recovery.inverse()
        val beta = gamma.multiply(R_0)
        val T = 100L

        // some matrix indices
        val i_c = EpiPart.values().size.toLong()
        val i_n = i_c + 2
        val i_S = EpiPart.S.ordinal.toLong()
        val i_I = EpiPart.I.ordinal.toLong()
        val i_R = EpiPart.R.ordinal.toLong()
        val i_SI = i_n - 2
        val i_N = i_n - 1

        // initialize population structure
        val rates =
            sparseMatrix {
                size = i_n by i_c
                label = "Rates"
                label(i_S, "S", "dS")
                label(i_I, "I", "dI")
                label(i_R, "R", "dR")
                label(i_SI, "SI/N")
                label(i_N, "N")
            }
            // infection: beta * SI/N, flows from S to I
            .subtract(beta, i_SI, i_S)
            .add(beta, i_SI, i_I)
            // recovery: gamma * I, flows from I to R
            .subtract(gamma, i_I, i_I)
            .add(gamma, i_I, i_R)

        // row-vector of population's compartments
        val population: Matrix =
            sparseMatrix {
                size = 1 by i_c
                label = "Population"
            }
            .put(mapOf(EpiPart.S to 999, EpiPart.I to 1, EpiPart.R to 0), 0, 0)

        // row-vector for updating SIR's ordinal differential equation (ODE) terms
        val terms: Matrix =
            sparseMatrix {
                size = 1 by i_n
                label = "Terms"
                label(i_SI, "SI/N")
                label(i_N, "N")
                labelColumns(*EpiPart.values())
            }

        val total: BigDecimal = population.sum()

        println("reproduction rate (R_0): $R_0, recovery period: $recovery, beta: $beta, gamma: $gamma, dt: $dt_ratio, population (N): $total, M_rate:\n$rates")

        val results: Matrix =
            sparseMatrix {
                valueType = ValueType.BIGDECIMAL
                size = T by 2 * i_c
                label = "Forw.Euler"
                rowLabeler = { i -> "t=" + dt.multiply(BigDecimal(i)) }
                columnLabeler = { i -> ((if (i < i_c) "" else "d") + EpiPart.values()[(i % i_c).toInt()]) }
            }
            .put(population, 0, 0)

        for (t in 1 until T) {

            val newSIoverN = population.get(0, i_S).multiply(population.get(0, i_I)).divide(total)

            // update terms
            terms.put(population).put(newSIoverN, 0, i_SI).put(total, 0, i_N)

            // calculate deltas
            val deltas: Matrix = terms.mtimes(rates).multiply(dt)

            // store results
            results.put(deltas, t - 1, i_c)
            results.put(population.add(deltas), t, 0)
        }
        println("Results: \n$results")
    }
}