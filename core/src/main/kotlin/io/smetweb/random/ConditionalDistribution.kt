package io.smetweb.random

import java.util.NavigableMap

interface ConditionalDistribution<T, C> {

	fun draw(condition: C): T

	companion object {

		@JvmStatic
		fun <T, C> of(distGen: (C) -> ProbabilityDistribution<T>): ConditionalDistribution<T, C> =
			of(param1Gen = { it }, distGen = distGen)

		/**
		 * @param [T] the type of value drawn by the conditional distributions
		 * @param [C] the type of condition for selecting a distribution
		 * @param [X] the type of parameter for generating a distribution
		 * @param distGen distribution generator taking one parameter
		 * @param param1Gen generator of the first parameter given the condition
		 * @param distCache caches previously generated distributions
		 * @return a [ConditionalDistribution]
		 */
		@JvmStatic
		fun <T, C, X> of(
			distCache: MutableMap<C, ProbabilityDistribution<T>> = mutableMapOf(),
			param1Gen: (C) -> X,
			distGen: (X) -> ProbabilityDistribution<T>
		): ConditionalDistribution<T, C> =
			object: ConditionalDistribution<T, C> {
				override fun draw(condition: C): T =
					distCache.getOrPut(condition) { distGen(param1Gen(condition)) }.draw()
			}

		/**
		 * conditional Bernoulli distributions
		 *
		 * @param [C] the type of condition for selecting a distribution
		 * @param rng a [PseudoRandom] number generator
		 * @param probGen the probability generator, e.g. a [Map]
		 * @return a [ConditionalDistribution]
		 */
		fun <C> bernoulli(rng: PseudoRandom, probGen: (C) -> Number): ConditionalDistribution<Boolean, C> =
			of(param1Gen = probGen) { p: Number -> rng.bernoulli(p) }

		/**
		 * @param [T] the type of value drawn by the conditional distributions
		 * @param [C] the type of condition for selecting a distribution
		 * @param [X] the type of parameter for generating a distribution
		 * @param distGen distribution generator taking one parameter
		 * @param paramMap a [Map] of the first parameter per condition
		 * @return a [ConditionalDistribution]
		 */
		fun <T, C, X> of(
			paramMap: Map<C, X>,
			distGen: (X) -> ProbabilityDistribution<T>
		): ConditionalDistribution<T, C> =
			if (paramMap.isEmpty())
				throw IllegalArgumentException("empty")
			else
				of(param1Gen = paramMap::getValue, distGen = distGen)

		/**
		 * @param [T] the type of value drawn by the conditional distributions
		 * @param [C] the type of condition for selecting a distribution
		 * @param [X] the type of parameter for generating a distribution
		 * @param distGen distribution generator taking one parameter
		 * @param paramMap a [NavigableMap] of the nearest first parameter
		 * given the condition using [NavigableMap.floorEntry]
		 * @return a [ConditionalDistribution]
		 */
		fun <T, C, X> of(
			paramMap: NavigableMap<C, out X>,
			distGen: (X) -> ProbabilityDistribution<T>
		): ConditionalDistribution<T, C> =
			if (paramMap.isEmpty())
				throw IllegalArgumentException("empty")
			else
				of(param1Gen = { c: C ->
					(paramMap.floorEntry(c) ?: paramMap.firstEntry()).value
				}, distGen = distGen)
	}
}