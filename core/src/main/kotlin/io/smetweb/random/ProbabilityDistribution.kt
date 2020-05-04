package io.smetweb.random

import io.smetweb.math.toDecimal
import java.math.BigDecimal
import java.util.function.Supplier
import java.util.stream.Stream

interface ProbabilityDistribution<T>: Supplier<T> {

	fun draw(): T

	override fun get(): T = draw()

	companion object {

		/**
		 * @param <T> the type of value
		 * @param value the constant to be returned on each draw
		 * @return a degenerate or deterministic {@link ProbabilityDistribution}
		 */
		@JvmStatic
		fun <T> deterministic(value: T): ProbabilityDistribution<T> =
                deterministic(value)

		/**
		 * @param probability the probability of drawing `true`
		 * @return a Bernoulli [ProbabilityDistribution]
		 */
		@JvmStatic
		fun bernoulli(rng: PseudoRandom, probability: Number): ProbabilityDistribution<Boolean> =
				rng.bernoulli(probability)

		/**
		 * Multi-noulli definitions by
		 * [Wikipedia](https://www.wikiwand.com/en/Categorical_distribution) and
		 * [Wolfram](https://www.wolframalpha.com/input/?i=bernoulli+distribution)
		 *
		 * <img alt="Probability density function" src=https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/2D-simplex.svg/440px-2D-simplex.svg.png>
		 *
		 * @param <V> the type of value to draw
		 * @param pmf the probability mass function mapping all possible observations to their probabilities
		 * @return a categorical [ProbabilityDistribution]
		</T> */
		@JvmStatic
		fun <V: Any, W: Number> categorical(rng: PseudoRandom, pmf: Iterable<Map.Entry<V, W>>): ProbabilityDistribution<V> =
				rng.categorical(pmf.map {(v, w) -> Pair(v, w.toDecimal())})

		@JvmStatic
		fun triangular(rng: PseudoRandom, min: Number, mode: Number, max: Number): ProbabilityDistribution<BigDecimal> =
				rng.triangular(min = min, mode = mode, max = max)

		@JvmStatic
		fun empirical(rng: PseudoRandom, observations: Stream<out Number>, binCount: Int = 1): ProbabilityDistribution<Double> =
				rng.empirical(observations, binCount)

	}
}