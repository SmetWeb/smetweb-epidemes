package io.smetweb.math

import java.util.function.Supplier

interface ProbabilityDistribution<T>: Supplier<T> {

	fun draw(): T

	override fun get(): T = draw()

	interface Factory {
		val stream: PseudoRandom
	}

	companion object {

		/**
		 * @param <T> the type of value
		 * @param value the constant to be returned on each draw
		 * @return a degenerate or deterministic {@link ProbabilityDistribution}
		 */
		@JvmStatic
		fun <T> createDeterministic(value: T): ProbabilityDistribution<T> =
				object: ProbabilityDistribution<T> {
					override fun draw(): T = value
				}

		/**
		 * @param rng the [PseudoRandom] number generator
		 * @param probability the probability of drawing [Boolean.TRUE]
		 * @return a [Bernoulli]() [ProbabilityDistribution]
		 */
		@JvmStatic
		fun createBernoulli(rng: PseudoRandom, probability: Number): ProbabilityDistribution<Boolean> =
				object: ProbabilityDistribution<Boolean> {
					override fun draw(): Boolean =
							rng.nextDouble() < probability.toDouble()
				}
	}
}