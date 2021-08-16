package io.smetweb.random

import cern.jet.random.engine.MersenneTwister64
import cern.jet.random.engine.RandomEngine
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
				"JDK" to Function<Long, PseudoRandom> { PseudoRandomJava(it) },
				"ECJ MT" to Function { PseudoRandomEcj(it) },
				"Commons MT" to Function { PseudoRandomCommons(it) },
				"Colt MT" to Function { PseudoRandomColt(it) },
				"DSOL MT" to Function { PseudoRandomDsol(it) },
				"Kotlin" to Function { PseudoRandomKotlin(it) })
				.forEach { (name, init) ->
					val rng1: PseudoRandom = init.apply(seed)
					val rng2: PseudoRandom = init.apply(seed)
					Assertions.assertEquals(rng1.nextDouble(), rng2.nextDouble())
					val t1 = repeat(rng1, n)
					val t2 = repeat(rng2, n)
					println(String.format("%12s : %4dms + %4dms", name, t1, t2))
				}
	}


	class PseudoRandomColt(
		override val seed: Long = System.currentTimeMillis() xor System.nanoTime(),
		private val rng: RandomEngine = MersenneTwister64(seed.toInt())
	): PseudoRandom {

		override fun nextBoolean(): Boolean =
			this.rng.raw() < 0.5

		override fun nextDouble(): Double =
			this.rng.nextDouble()

		override fun nextFloat(): Float =
			this.rng.nextFloat()

		override fun nextInt(): Int =
			this.rng.nextInt()

		override fun nextLong(): Long =
			this.rng.nextLong()
	}
}
