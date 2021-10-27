package io.smetweb.random

import org.apache.commons.math3.distribution.*
import org.apache.commons.math3.exception.MathIllegalNumberException
import org.apache.commons.math3.exception.OutOfRangeException
import org.apache.commons.math3.random.RandomGenerator

/** @return a [PseudoRandom] wrapping a [RandomGenerator] from the `commons-math3` library */
fun RandomGenerator.toPseudoRandom(): PseudoRandom = object: PseudoRandom {
	override val seed get() = error("can't access seed of ${this@toPseudoRandom}")
	override fun nextBoolean(): Boolean = this@toPseudoRandom.nextBoolean()
	override fun nextBytes(bytes: ByteArray): ByteArray = bytes.apply { this@toPseudoRandom.nextBytes(this) }
	override fun nextDouble(): Double = this@toPseudoRandom.nextDouble()
	override fun nextGaussian(): Double  = this@toPseudoRandom.nextGaussian()
	override fun nextFloat(): Float = this@toPseudoRandom.nextFloat()
	override fun nextInt(): Int = this@toPseudoRandom.nextInt()
	override fun nextIntBelow(boundIncl: Int): Int = this@toPseudoRandom.nextInt(boundIncl)
	override fun nextLong(): Long = this@toPseudoRandom.nextLong()
}

/** @return a [RandomGenerator] from the `commons-math3` library wrapping a [PseudoRandom] */
fun PseudoRandom.toRandomGenerator(): RandomGenerator = object: RandomGenerator {
	override fun setSeed(seed: Int) = error("Can't reset seed of ${this@toRandomGenerator}")
	override fun setSeed(seed: IntArray) = error("Can't reset seed of ${this@toRandomGenerator}")
	override fun setSeed(seed: Long) = error("Can't reset seed of ${this@toRandomGenerator}")
	override fun nextBytes(bytes: ByteArray) { this@toRandomGenerator.nextBytes(bytes) }
	override fun nextInt(): Int = this@toRandomGenerator.nextInt()
	override fun nextInt(n: Int): Int = this@toRandomGenerator.nextIntBelow(n)
	override fun nextLong(): Long = this@toRandomGenerator.nextLong()
	override fun nextBoolean(): Boolean = this@toRandomGenerator.nextBoolean()
	override fun nextFloat(): Float = this@toRandomGenerator.nextFloat()
	override fun nextDouble(): Double = this@toRandomGenerator.nextDouble()
	override fun nextGaussian(): Double = this@toRandomGenerator.nextGaussian()
}

/**
 * see also: [binomial distribution](https://www.wikiwand.com/en/Binomial_distribution)
 * @return a [ProbabilityDistribution] wrapping a [BinomialDistribution] from the `commons-math3` library
 */
@Throws(OutOfRangeException::class)
fun PseudoRandom.commonsBinomial(trials: Number, probabilityOfSuccess: Number): ProbabilityDistribution<Int> {
	val dist = BinomialDistribution(toRandomGenerator(), trials.toInt(), probabilityOfSuccess.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [geometric distribution](https://www.wikiwand.com/en/Geometric_distribution)
 * @return a [ProbabilityDistribution] wrapping a [GeometricDistribution] from the `commons-math3` library
 */
@Throws(OutOfRangeException::class)
fun PseudoRandom.commonsGeometric(probabilityOfSuccess: Number): ProbabilityDistribution<Int> {
	val dist = GeometricDistribution(toRandomGenerator(), probabilityOfSuccess.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [hypergeometric distribution](https://www.wikiwand.com/en/Hypergeometric_distribution)
 * @return a [ProbabilityDistribution] wrapping a [HypergeometricDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsHypergeometric(populationSize: Number, numberOfSuccesses: Number, sampleSize: Number): ProbabilityDistribution<Int> {
	val dist = HypergeometricDistribution(toRandomGenerator(), populationSize.toInt(), numberOfSuccesses.toInt(), sampleSize.toInt())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [negative binomial distribution](https://www.wikiwand.com/en/Negative_binomial_distribution)
 * @return a [ProbabilityDistribution] wrapping a [PascalDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsPascal(numberOfSuccesses: Number, probabilityOfSuccess: Number): ProbabilityDistribution<Int> {
	val dist = PascalDistribution(toRandomGenerator(), numberOfSuccesses.toInt(), probabilityOfSuccess.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [Poisson distribution](https://www.wikiwand.com/en/Poisson_distribution)
 * @return a [ProbabilityDistribution] wrapping a [PoissonDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsPoisson(
	probabilityOfSuccess: Number,
	epsilon: Number = PoissonDistribution.DEFAULT_EPSILON,
	maxIterations: Number = PoissonDistribution.DEFAULT_MAX_ITERATIONS
): ProbabilityDistribution<Int> {
	val dist = PoissonDistribution(toRandomGenerator(), probabilityOfSuccess.toDouble(), epsilon.toDouble(), maxIterations.toInt())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [Zipf's law](https://www.wikiwand.com/en/Zipf's_law)
 * @return a [ProbabilityDistribution] wrapping a [ZipfDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsZipf(numberOfElements: Number, exponents: Number): ProbabilityDistribution<Int> {
	val dist = ZipfDistribution(toRandomGenerator(), numberOfElements.toInt(), exponents.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [beta distribution](https://www.wikiwand.com/en/Beta_distribution)
 * @return a [ProbabilityDistribution] wrapping a [BetaDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsBeta(
	alpha: Number,
	beta: Number,
	inverseCumAccuracy: Number = BetaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = BetaDistribution(toRandomGenerator(), alpha.toDouble(), beta.toDouble(), inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [Cauchy distribution](https://www.wikiwand.com/en/Cauchy_distribution)
 * @return a [ProbabilityDistribution] wrapping a [CauchyDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsCauchy(
	median: Number,
	scale: Number,
	inverseCumAccuracy: Number = CauchyDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = CauchyDistribution(toRandomGenerator(), median.toDouble(), scale.toDouble(), inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [χ2 distribution](https://www.wikiwand.com/en/Chi-squared_distribution)
 * @return a [ProbabilityDistribution] wrapping a [ChiSquaredDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsChiSquared(
	degreesOfFreedom: Number,
	inverseCumAccuracy: Number = ChiSquaredDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = ChiSquaredDistribution(toRandomGenerator(), degreesOfFreedom.toDouble(), inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [exponential distribution](https://www.wikiwand.com/en/Exponential_distribution)
 * @return a [ProbabilityDistribution] wrapping a [ExponentialDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsExponential(
	mean: Number,
	inverseCumAccuracy: Number = ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = ExponentialDistribution(toRandomGenerator(), mean.toDouble(), inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [F-distribution](https://www.wikiwand.com/en/F-distribution)
 * @return a [ProbabilityDistribution] wrapping a [FDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsF(
	numeratorDegreesOfFreedom: Number,
	denominatorDegreesOfFreedom: Number,
	inverseCumAccuracy: Number = FDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = FDistribution(toRandomGenerator(), numeratorDegreesOfFreedom.toDouble(),
		denominatorDegreesOfFreedom.toDouble(), inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [gamma distribution](https://www.wikiwand.com/en/Gamma_distribution)
 * @return a [ProbabilityDistribution] wrapping a [GammaDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsGamma(
	shape: Number,
	scale: Number,
	inverseCumAccuracy: Number = GammaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = GammaDistribution(toRandomGenerator(), shape.toDouble(), scale.toDouble(), inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [Lévy distribution](https://www.wikiwand.com/en/Lévy_distribution)
 * @return a [ProbabilityDistribution] wrapping a [LevyDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsLevy(mu: Number, c: Number): ProbabilityDistribution<Double> {
	val dist = LevyDistribution(toRandomGenerator(), mu.toDouble(), c.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [log-normal distribution](https://www.wikiwand.com/en/Log-normal_distribution)
 * @return a [ProbabilityDistribution] wrapping a [LogNormalDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsLogNormal(
	scale: Number,
	shape: Number,
	inverseCumAccuracy: Number = LogNormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = LogNormalDistribution(toRandomGenerator(), scale.toDouble(), shape.toDouble(), inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [normal distribution](https://www.wikiwand.com/en/Normal_distribution)
 * @return a [ProbabilityDistribution] wrapping a [NormalDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsNormal(
	mean: Number,
	stDev: Number,
	inverseCumAccuracy: Number = NormalDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = NormalDistribution(toRandomGenerator(), mean.toDouble(), stDev.toDouble(), inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [Pareto distribution](https://www.wikiwand.com/en/Pareto_distribution)
 * @return a [ProbabilityDistribution] wrapping a [ParetoDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsPareto(
	scale: Number,
	shape: Number,
	inverseCumAccuracy: Number = ParetoDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = ParetoDistribution(toRandomGenerator(), scale.toDouble(), shape.toDouble(),inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [Student's T-distribution](https://www.wikiwand.com/en/Student's_t-distribution)
 * @return a [ProbabilityDistribution] wrapping a [TDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsT(
	degreesOfFreedom: Number,
	inverseCumAccuracy: Number = TDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = TDistribution(toRandomGenerator(), degreesOfFreedom.toDouble(), inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [discrete uniform distribution](https://www.wikiwand.com/en/Discrete_uniform_distribution)
 * @return a [ProbabilityDistribution] wrapping a [UniformIntegerDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsUniformInt(lowerIncl: Number, upperIncl: Number): ProbabilityDistribution<Int> {
	val dist = UniformIntegerDistribution(toRandomGenerator(), lowerIncl.toInt(), upperIncl.toInt())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [continuous uniform distribution](https://www.wikiwand.com/en/Continuous_uniform_distribution)
 * @return a [ProbabilityDistribution] wrapping a [UniformRealDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsUniformReal(lowerIncl: Number, upperExcl: Number): ProbabilityDistribution<Double> {
	val dist = UniformRealDistribution(toRandomGenerator(), lowerIncl.toDouble(), upperExcl.toDouble())
	return probabilityDistribution { dist.sample() }
}

/**
 * see also: [Weibull distribution](https://www.wikiwand.com/en/Weibull_distribution)
 * @return a [ProbabilityDistribution] wrapping a [WeibullDistribution] from the `commons-math3` library
 */
@Throws(MathIllegalNumberException::class)
fun PseudoRandom.commonsWeibull(
	alpha: Number,
	beta: Number,
	inverseCumAccuracy: Number = WeibullDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY
): ProbabilityDistribution<Double> {
	val dist = WeibullDistribution(toRandomGenerator(), alpha.toDouble(), beta.toDouble(), inverseCumAccuracy.toDouble())
	return probabilityDistribution { dist.sample() }
}
