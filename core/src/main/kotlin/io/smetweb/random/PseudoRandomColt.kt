package io.smetweb.random

import cern.jet.random.engine.MersenneTwister64
import cern.jet.random.engine.RandomEngine

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