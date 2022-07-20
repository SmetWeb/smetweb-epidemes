package io.smetweb.random

import cern.jet.random.engine.RandomEngine
import io.smetweb.sim.dsol.toPseudoRandom
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.function.Function
import kotlin.system.measureTimeMillis

class PseudoRandomTest {

	private fun repeat(rng: PseudoRandom, n: Int): Long = measureTimeMillis {
		repeat(n) {
			rng.nextBoolean()
			rng.nextDouble()
			rng.nextGaussian()
		}
	}

	@Test
	fun `seed consistency` () {
		val seed = 4357L
		val n = 1_000_000
		mapOf(
				"JDK" to Function<Long, PseudoRandom> { java.util.Random(it).toPseudoRandom() },
				"ECJ MT" to Function { PseudoRandomEcj(it) },
				"Commons MT" to Function { org.apache.commons.math3.random.MersenneTwister(it).toPseudoRandom() },
				"Colt MT" to Function { cern.jet.random.engine.MersenneTwister64(it.toInt()).toPseudoRandom() },
				"DSOL MT" to Function { nl.tudelft.simulation.jstats.streams.MersenneTwister(it).toPseudoRandom() },
				"Kotlin" to Function { kotlin.random.Random(it).toPseudoRandom() })
				.forEach { (name, init) ->
					val rng1: PseudoRandom = init.apply(seed)
					val rng2: PseudoRandom = init.apply(seed)
					Assertions.assertEquals(rng1.nextDouble(), rng2.nextDouble())
					val t1 = repeat(rng1, n)
					val t2 = repeat(rng2, n)
					println(String.format("%12s : %4dms + %4dms", name, t1, t2))
				}
	}

	fun RandomEngine.toPseudoRandom() = object: PseudoRandom {
		override val seed: Number get() = error("Can't access seed in ${this@toPseudoRandom}")
		override fun nextBoolean(): Boolean = this@toPseudoRandom.raw() < 0.5
		override fun nextDouble(): Double = this@toPseudoRandom.nextDouble()
		override fun nextFloat(): Float = this@toPseudoRandom.nextFloat()
		override fun nextInt(): Int = this@toPseudoRandom.nextInt()
		override fun nextLong(): Long = this@toPseudoRandom.nextLong()
	}
}
