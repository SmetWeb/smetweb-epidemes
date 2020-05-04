package io.smetweb.random

import kotlin.random.Random

class PseudoRandomKotlin(
		override val seed: Long = System.currentTimeMillis() xor System.nanoTime(),
		private val rng: Random = Random(seed)
): PseudoRandom {

	override fun nextBoolean(): Boolean = this.rng.nextBoolean()

	override fun nextBytes(bytes: ByteArray) = this.rng.nextBytes(bytes)

	override fun nextInt(): Int = this.rng.nextInt()

	override fun nextIntBelow(bound: Int): Int = this.rng.nextInt(bound)

	override fun nextLong(): Long = this.rng.nextLong()

	override fun nextLongBelow(bound: Long): Long = this.rng.nextLong(bound)

	override fun nextFloat(): Float = this.rng.nextFloat()

	override fun nextDouble(): Double = this.rng.nextDouble()

}