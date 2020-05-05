package io.smetweb.random

import nl.tudelft.simulation.jstats.streams.MersenneTwister
import nl.tudelft.simulation.jstats.streams.RandomNumberGenerator

class PseudoRandomDsol(
		override val seed: Long = System.currentTimeMillis() xor System.nanoTime(),
		private val rng: RandomNumberGenerator = MersenneTwister(seed)
): PseudoRandom {

	override fun nextBoolean(): Boolean = this.rng.nextBoolean()

	override fun nextInt(): Int = this.rng.nextInt()

	override fun nextIntBelow(bound: Int): Int = this.rng.nextInt(0, bound)

	override fun nextLong(): Long = this.rng.nextLong()

	override fun nextFloat(): Float = this.rng.nextFloat()

	override fun nextDouble(): Double = this.rng.nextDouble()

}