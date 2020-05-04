package io.smetweb.random

import java.util.*

class PseudoRandomJava(
		override val seed: Long = System.currentTimeMillis() xor System.nanoTime(),
		private val rng: Random = Random(seed)
): PseudoRandom {

	override fun nextBoolean(): Boolean = this.rng.nextBoolean()

	override fun nextBytes(bytes: ByteArray): ByteArray {
		this.rng.nextBytes(bytes)
		return bytes
	}

	override fun nextInt(): Int = this.rng.nextInt()

	override fun nextIntBelow(bound: Int): Int = this.rng.nextInt(bound)

	override fun nextLong(): Long = this.rng.nextLong()

	override fun nextFloat(): Float = this.rng.nextFloat()

	override fun nextDouble(): Double = this.rng.nextDouble()

	override fun nextGaussian(): Double = this.rng.nextGaussian()

}