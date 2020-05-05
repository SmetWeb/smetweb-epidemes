package io.smetweb.random

import org.apache.commons.math3.random.MersenneTwister
import org.apache.commons.math3.random.RandomGenerator

class PseudoRandomCommons(
        override val seed: Long = System.currentTimeMillis() xor System.nanoTime(),
        private val rng: RandomGenerator = MersenneTwister(seed)
): PseudoRandom {

    override fun nextBoolean(): Boolean =
            this.rng.nextBoolean()

    override fun nextBytes(bytes: ByteArray): ByteArray {
        this.rng.nextBytes(bytes)
        return bytes
    }

    override fun nextDouble(): Double =
            this.rng.nextDouble()

    override fun nextGaussian(): Double  =
            this.rng.nextGaussian()

    override fun nextFloat(): Float =
            this.rng.nextFloat()

    override fun nextInt(): Int =
            this.rng.nextInt()

    override fun nextIntBelow(bound: Int): Int =
            this.rng.nextInt(bound)

    override fun nextLong(): Long =
            this.rng.nextLong()
}