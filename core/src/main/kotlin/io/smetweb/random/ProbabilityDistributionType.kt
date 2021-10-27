package io.smetweb.random

import io.smetweb.math.parseQuantity
import io.smetweb.math.toDecimal
import java.math.BigDecimal
import java.text.ParseException
import java.util.*
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Dimensionless

enum class ProbabilityDistributionType(private vararg val symbols: String) {

	/** see also: [degenerate distribution](https://www.wikiwand.com/en/Degenerate_distribution) */
	DEGENERATE("const", "constant", "degen", "degenerate", "det", "determ", "deterministic"),

	/** see also: [Bernoulli distribution](https://www.wikiwand.com/en/Bernoulli_distribution) */
	BERNOULLI("p", "bool", "bernoulli"),

	/** see also: [binomial distribution](https://www.wikiwand.com/en/Binomial_distribution) */
	BINOMIAL("binom", "binomial"),

	/** see also: [categorical distribution](https://www.wikiwand.com/en/Categorical_distribution) */
	CATEGORICAL("categorical", "enum", "enumerated", "multinoulli",
		"uniform-enum", "uniform-enumerated", "uniform-categorical"),

	/** see also: [geometric distribution](https://www.wikiwand.com/en/Geometric_distribution) */
	GEOMETRIC("geom", "geometric"),

	/** see also: [hypergeometric distribution](https://www.wikiwand.com/en/Hypergeometric_distribution) */
	HYPERGEOMETRIC("hypergeom", "hypergeometric"),

	/** see also: [negative binomial distribution](https://www.wikiwand.com/en/Negative_binomial_distribution) */
	PASCAL("pascal", "neg-binom", "negative-binomial"),

	/** see also: [Poisson distribution](https://www.wikiwand.com/en/Poisson_distribution) */
	POISSON("poisson"),

	/** see also: [Zipf's law](https://www.wikiwand.com/en/Zipf's_law) */
	ZIPF("zipf"),

	/** see also: [beta distribution](https://www.wikiwand.com/en/Beta_distribution) */
	BETA("beta"),

	/** see also: [Cauchy distribution](https://www.wikiwand.com/en/Cauchy_distribution) */
	CAUCHY("cauchy", "cauchy-lorentz", "lorentz", "lorentzian", "breit-wigner"),

	/** see also: [χ2 distribution](https://www.wikiwand.com/en/Chi-squared_distribution) */
	CHI_SQUARED("chi", "chisquare", "chisquared", "chi-square", "chi-squared"),

	/** see also: [exponential distribution](https://www.wikiwand.com/en/Exponential_distribution) */
	EXPONENTIAL("exp", "exponent", "exponential"),

	/** see also: [empirical distribution function](https://www.wikiwand.com/en/Empirical_distribution_function) */
	EMPIRICAL("emp", "empirical"),

	/** see also: [F-distribution](https://www.wikiwand.com/en/F-distribution) */
	BETA_PRIME("pearson6", "beta-prime", "inverted-beta", "f", "snedecor-f", "fisher-snedecor"),

	/** see also: [gamma distribution](https://www.wikiwand.com/en/Gamma_distribution) */
	GAMMA("pearson3", "erlang", "gamma"),

	/** see also: [Lévy distribution](https://www.wikiwand.com/en/Lévy_distribution) */
	LEVY("levy"),

	/** see also: [log-normal distribution](https://www.wikiwand.com/en/Log-normal_distribution) */
	LOG_NORMAL("lognormal", "log-normal", "gibrat"),

	/** see also: [normal distribution](https://www.wikiwand.com/en/Normal_distribution) */
	NORMAL("gauss", "gaussian", "normal", "laplace-gauss"),

	/** see also: [Pareto distribution](https://www.wikiwand.com/en/Pareto_distribution) */
	PARETO("pareto", "pareto1"),

	/** see also: [Student's T-distribution](https://www.wikiwand.com/en/Student's_t-distribution) */
	STUDENTS_T("students-t", "t"),

	/** see also: [triangular distribution](https://www.wikiwand.com/en/Triangular_distribution) */
	TRIANGULAR("tria", "triangular"),

	/** see also: [discrete uniform distribution](https://www.wikiwand.com/en/Discrete_uniform_distribution) */
	UNIFORM_DISCRETE("uniform-discrete", "uniform-integer"),

	/** see also: [continuous uniform distribution](https://www.wikiwand.com/en/Continuous_uniform_distribution) */
	UNIFORM_CONTINUOUS("uniform", "uniform-real", "uniform-continuous"),

	/** see also: [Weibull distribution](https://www.wikiwand.com/en/Weibull_distribution) */
	WEIBULL("frechet", "weibull"),

	;

	companion object {

		private val mapping: Map<String, ProbabilityDistributionType> by lazy {
			TreeMap<String, ProbabilityDistributionType>().apply {
				values().forEach { distType ->
					distType.symbols.forEach { this[it] = distType }
				}
			}
		}

		/**
		 * the PARAM_SEPARATORS exclude comma character `','` due to its
		 * common use as separator of decimals (e.g. `XX,X`) or of
		 * thousands (e.g. `n,nnn,nnn.nn`)
		 */
		private const val PARAM_SEPARATORS = ";"
		private const val WEIGHT_SEPARATORS = ":"
		private const val DIST_GROUP = "dist"
		private const val PARAMS_GROUP = "params"

		/**
		 * matches string representations like:
		 * `dist(arg1; arg2; )` or
		 * `dist(v1:w1; v2:w2; )`
		 */
		private val DISTRIBUTION_FORMAT = ("^(?<$DIST_GROUP>[^\\(]+)\\s*\\((?<$PARAMS_GROUP>[^)]*)\\)$").toPattern()

		/**
		 * parse this [CharSequence] assuming a formatting `"dist(arg1; arg2; ...)"` to represent a [ProbabilityDistribution] of
		 * specific shape, probability mass (discrete), or density (continuous) function. For instance:
		 * - `"gauss(mean, stDev)"` or
		 * - `"enum(val1:w1; val2:w2; ...)"`
		 *
		 * @param T parsed argument type
		 * @param R drawn value type: discrete/continuous [Number] or [Quantity], categorical (e.g. [Boolean], [Enum]), etc.
		 * @param rng the [PseudoRandom] number generator (PRNG) used for sampling from the parsed [ProbabilityDistribution]
		 * @param argParser the argument/value parser, defaults to [Quantity] format (with [Unit] or numeric/[Dimensionless])
		 * @param argNormaliser the argument/value normaliser, defaults to 'as-is'
		 * @return a [ProbabilityDistribution] drawing values of type [R]
		 */
		@Suppress("UNCHECKED_CAST")
		@Throws(ParseException::class)
		fun <T: Any, R: Any> parseProbabilityDistribution(
			csq: CharSequence,
			rng: PseudoRandom,
			argParser: (CharSequence) -> T = { it.parseQuantity() as T },
			argNormaliser: (List<Pair<T, BigDecimal>>) -> List<Pair<T, BigDecimal>> = { it }
		): ProbabilityDistribution<R> {
			val m = DISTRIBUTION_FORMAT.matcher(csq.trim { it <= ' ' })
			if (!m.find())
				throw ParseException("Incorrect format, expected <dist>(p0[:w0][;p1[:w1][;...]]), got: $this", 0)

			val distName = m.group(DIST_GROUP).trim { it <= ' ' }.lowercase(Locale.ROOT)
			val distArgs: List<Pair<T, BigDecimal>> = m.group(PARAMS_GROUP)
				.split(PARAM_SEPARATORS)
				.filter { param -> param.trim { it <= ' ' }.isNotEmpty() }
				.map { param ->
					val pair = param.split(WEIGHT_SEPARATORS)
					when (pair.size) {
						2 -> Pair(argParser(pair[0]), BigDecimal(pair[1].trim { it <= ' ' }))
						// assume equal weight
						else -> Pair(argParser(param), BigDecimal.ONE)
					}
				}

			val params = argNormaliser(distArgs)
			if (params.isEmpty())
				throw ParseException("Missing parameters for `$distName(...)`, was: $this", csq.length)

			return when (mapping[distName]) {
				DEGENERATE -> deterministic(params[0].first)
				BERNOULLI -> rng.bernoulli(probabilityOfSuccess = params[0].firstAsNumber())
				BINOMIAL -> rng.commonsBinomial(
					trials = params[0].firstAsNumber(),
					probabilityOfSuccess = params[1].firstAsNumber())
				CATEGORICAL -> rng.categorical(pmf = params)
				GEOMETRIC -> rng.commonsGeometric(probabilityOfSuccess = params[0].firstAsNumber())
				HYPERGEOMETRIC -> rng.commonsHypergeometric(
					populationSize = params[0].firstAsNumber(),
					numberOfSuccesses = params[1].firstAsNumber(),
					sampleSize = params[2].firstAsNumber())
				PASCAL -> rng.commonsPascal(
					numberOfSuccesses = params[0].firstAsNumber(),
					probabilityOfSuccess = params[1].firstAsNumber())
				POISSON -> rng.commonsPoisson(probabilityOfSuccess = params[0].firstAsNumber())
				ZIPF -> rng.commonsZipf(
					numberOfElements = params[0].firstAsNumber(),
					exponents = params[1].firstAsNumber())
				BETA -> rng.commonsBeta(alpha = params[0].firstAsNumber(), beta = params[1].firstAsNumber())
				CAUCHY -> rng.commonsCauchy(median = params[0].firstAsNumber(), scale = params[1].firstAsNumber())
				CHI_SQUARED -> rng.commonsChiSquared(degreesOfFreedom = params[0].firstAsNumber())
				EXPONENTIAL -> rng.commonsExponential(mean = params[0].firstAsNumber())
				BETA_PRIME -> rng.commonsF(
					numeratorDegreesOfFreedom = params[0].firstAsNumber(),
					denominatorDegreesOfFreedom = params[1].firstAsNumber())
				GAMMA -> rng.commonsGamma(shape = params[0].firstAsNumber(), scale = params[1].firstAsNumber())
				LEVY -> rng.commonsLevy(mu = params[0].firstAsNumber(), c = params[1].firstAsNumber())
				LOG_NORMAL -> rng.commonsLogNormal(scale = params[0].firstAsNumber(), shape = params[1].firstAsNumber())
				EMPIRICAL -> rng.empirical(observations = params.map(Pair<Any, *>::firstAsNumber).stream())
				NORMAL -> rng.normal(mean = params[0].firstAsNumber(), stDev = params[1].firstAsNumber())
				PARETO -> rng.commonsPareto(scale = params[0].firstAsNumber(), shape = params[1].firstAsNumber())
				STUDENTS_T -> rng.commonsT(degreesOfFreedom = params[0].firstAsNumber())
				TRIANGULAR -> rng.triangular(
					min = params[0].firstAsNumber(),
					mode = params[1].firstAsNumber(),
					max = params[2].firstAsNumber())
				UNIFORM_DISCRETE -> rng.uniform(
					min = params[0].firstAsNumber().toDecimal().toLong(),
					max = params[1].firstAsNumber().toDecimal().toLong())
				UNIFORM_CONTINUOUS -> rng.uniform(
					min = params[0].firstAsNumber().toDecimal().toDouble(),
					max = params[1].firstAsNumber().toDecimal().toDouble())
				WEIBULL -> rng.commonsWeibull(params[0].firstAsNumber(), params[1].firstAsNumber())
				else -> throw ParseException("Unknown distribution symbol: $this", 0)
			} as ProbabilityDistribution<R>
		}
	}
}