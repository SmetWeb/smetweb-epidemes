package io.smetweb.random

import io.smetweb.math.CONVERTERS
import io.smetweb.math.toUnit
import io.smetweb.time.ManagedClockService
import org.junit.jupiter.api.Test
import tech.units.indriya.unit.Units
import javax.measure.quantity.Time
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class DistributionParserTest {

	@Test
	fun `distribution symbols` () {
		val seed = 4357L
		val n = 10_000
		val rng = Random(seed).toPseudoRandom()

		val distBernoulli = "p(.5)".parseValueDistribution<Boolean>(rng)
		val time1 = measureTimeMillis {
			repeat(n) {
				val v = distBernoulli.draw()
				if (it % 1000 == 0)
					println("#$it: $v")
			}
		}
		println("drawing $n times took $time1 ms")

		val distCategorical = "enum(STARTeD:1;stopped:3)".parseEnumDistribution(rng, ManagedClockService.ClockStatus::class.java)
		val time3 = measureTimeMillis {
			repeat(n) {
				val v = distCategorical.draw()
				if (it % 1000 == 0)
					println("#$it: $v")
			}
		}
		println("drawing $n times took $time3 ms")

		val distConstant = "normal(-2 year; 2 year)".parseQuantityDistribution<Time>(rng)
		val time0 = measureTimeMillis {
			repeat(n) {
				val v = distConstant.draw().toUnit(Units.DAY)
				if (it % 1000 == 0)
					println("#$it: $v")
			}
		}
		println("drawing $n times took $time0 ms, converters: $CONVERTERS")
	}

}