package io.smetweb.random

import io.smetweb.math.*
import tech.units.indriya.ComparableQuantity
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.text.ParseException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Dimensionless

fun <Q: Quantity<Q>> ProbabilityDistribution<*>.toQuantities(unit: Unit<Q>): QuantityDistribution<Q> =
    quantityDistribution {
        when (val v = draw()) {
            is Number ->
                v.toQuantity(unit)
            is Quantity<*> ->
                @Suppress("UNCHECKED_CAST")
                (v as Quantity<Q>).toUnit(unit)
            else ->
                error("Can't convert ${v!!::class.java} $v to a Quantity")
        }
    }

fun <Q: Quantity<Q>> quantityDistribution(supplier: () -> ComparableQuantity<Q>): QuantityDistribution<Q> =
    object: QuantityDistribution<Q> {
        override fun draw(): ComparableQuantity<Q> = supplier()
    }

fun <T: Any> probabilityDistribution(supplier: () -> T): ProbabilityDistribution<T> =
    object: ProbabilityDistribution<T> {
        override fun draw(): T = supplier()
    }

fun <V: Any> PseudoRandom.toDistribution(transform: (PseudoRandom) -> V) =
    probabilityDistribution { transform(this@toDistribution) }

/**
 * see also: [degenerate distribution](https://www.wikiwand.com/en/Degenerate_distribution)
 * @return a (degenerate) deterministic [ProbabilityDistribution] that always draws given [constant] of type [T]
 */
fun <T: Any> deterministic(constant: T) =
    probabilityDistribution { constant }

/**
 * @param probabilityOfSuccess the probability &isin; &#91;0, 1] of drawing `true`
 * @return a Bernoulli [ProbabilityDistribution] that draws `true` with given [probabilityOfSuccess] using this [PseudoRandom]
 */
fun PseudoRandom.bernoulli(probabilityOfSuccess: Number): ProbabilityDistribution<Boolean> {
    val p = probabilityOfSuccess.toDouble()
    require(p >= 0) { "Negative probability: $probabilityOfSuccess" }
    return when(p) {
        0.0 -> deterministic(false)
        1.0 -> deterministic(true)
        else -> this.toDistribution { it.nextDouble() < p }
    }
}

/**
 * see also: [continuous uniform distribution](https://www.wikiwand.com/en/Discrete_uniform_distribution)
 * @return a uniform [ProbabilityDistribution] drawing from given (discrete) [values] of type [V] using this [PseudoRandom]
 */
fun <V: Any> PseudoRandom.uniform(values: Iterable<V>): ProbabilityDistribution<V> {
    val list: List<V> = values.toCollection(mutableListOf())
    return when(list.size) {
        0 -> throw IllegalArgumentException("No values")
        1 -> deterministic(list[0])
        else -> this.toDistribution { it.nextElement(list) }
    }
}

/**
 * see also: [continuous uniform distribution](https://www.wikiwand.com/en/Discrete_uniform_distribution)
 * @return a uniform [ProbabilityDistribution] drawing 64-bit [BigInteger] (discrete) values using this [PseudoRandom]
 */
@Throws(ArithmeticException::class)
fun PseudoRandom.uniform(min: BigInteger, max: BigInteger): ProbabilityDistribution<BigInteger> {
    require(min >= BigInteger.ZERO) { "min < 0" }
    require(max >= min) { "max = min" }
    val range: Long = max.subtract(min).longValueExact()
    return this.toDistribution { it.nextLongBelow(range).toBigInteger().add(min) }
}

/**
 * see also: [continuous uniform distribution](https://www.wikiwand.com/en/Continuous_uniform_distribution)
 * @return a continuous uniform [ProbabilityDistribution] drawing 64-bit/double-precision [BigDecimal] values using this [PseudoRandom]
 */
fun PseudoRandom.uniform(min: BigDecimal, max: BigDecimal): ProbabilityDistribution<BigDecimal> {
    require(min >= BigDecimal.ZERO) { "min < 0" }
    require(max >= min) { "max = min" }
    val range: BigDecimal = max.subtract(min)
    return this.toDistribution { it.nextDecimal().multiplyBy(range).add(min) }
}

/**
 * see also: [continuous uniform distribution](https://www.wikiwand.com/en/Discrete_uniform_distribution)
 * @return a uniform [ProbabilityDistribution] drawing 32-bit [Int] (discrete) values using this [PseudoRandom]
 */
fun PseudoRandom.uniform(min: Int, max: Int): ProbabilityDistribution<Int> {
    require(min >= 0) { "min < 0" }
    require(max >= min) { "max = min" }
    val range: Int = max - min
    return this.toDistribution { it.nextIntBelow(range) + min }
}

/**
 * see also: [discrete uniform distribution](https://www.wikiwand.com/en/Discrete_uniform_distribution)
 * @return a uniform [ProbabilityDistribution] drawing 64-bit [Long] (discrete) values using this [PseudoRandom]
 */
fun PseudoRandom.uniform(min: Long, max: Long): ProbabilityDistribution<Long> {
    require(min >= 0L) { "min < 0" }
    require(max > min) { "max =< min" }
    val range: Long = max - min
    return this.toDistribution { it.nextLongBelow(range) + min }
}

/**
 * see also: [continuous uniform distribution](https://www.wikiwand.com/en/Continuous_uniform_distribution)
 * @return a uniform [ProbabilityDistribution] drawing 32-bit [Float]ing-point (continuous) values using this [PseudoRandom]
 */
fun PseudoRandom.uniform(min: Float, max: Float): ProbabilityDistribution<Float> {
    require(min >= 0F) { "min < 0" }
    require(max >= min) { "max < min" }
    val range: Float = max - min
    return if (range <= 0)
        throw IllegalArgumentException("range: $min >= $max")
    else this.toDistribution { it.nextFloat() * range + min }
}

/**
 * see also: [continuous uniform distribution](https://www.wikiwand.com/en/Continuous_uniform_distribution)
 * @return a uniform [ProbabilityDistribution] drawing 64-bit/[Double]-precision floating-point values using this [PseudoRandom]
 */
fun PseudoRandom.uniform(min: Double, max: Double): ProbabilityDistribution<Double> {
    require(min >= 0.0) { "min < 0" }
    require(max >= min) { "max < min" }
    val range: Double = max - min
    return this.toDistribution { it.nextDouble() * range + min }
}

/**
 * see also: [normal distribution](https://www.wikiwand.com/en/Normal_distribution)
 * @return a normal [ProbabilityDistribution] drawing 64-bit/[Double]-precision floating-point values using this [PseudoRandom]
 */
fun PseudoRandom.normal(mean: Number, stDev: Number): ProbabilityDistribution<Double> {
    val meanDouble = mean.toDouble()
    val stDevDouble = stDev.toDouble()
    return this.toDistribution { it.nextGaussian() * stDevDouble + meanDouble }
}

/**
 * see also: [triangular distribution](https://www.wikiwand.com/en/Triangular_distribution)
 * @return a triangular [ProbabilityDistribution] drawing arbitrary-precision [BigDecimal] values using this [PseudoRandom]
 */
fun PseudoRandom.triangular(
    min: Number,
    mode: Number,
    max: Number,
    mathContext: MathContext = DEFAULT_CONTEXT
): ProbabilityDistribution<BigDecimal> {
    val modeBD = mode.toDecimal(mathContext)
    val minBD = min.toDecimal(mathContext)
    val maxBD = max.toDecimal(mathContext)
    val lower = modeBD.subtract(minBD, mathContext)
    val upper = maxBD.subtract(modeBD, mathContext)
    val total = maxBD.subtract(minBD, mathContext)
    val lowerDivTotal = lower.divideBy(total, mathContext)
    val lowerTimesTotal =lower.multiplyBy(total, mathContext)
    val upperTimesTotal = upper.multiplyBy(total, mathContext)
    return this.toDistribution {
        val p: BigDecimal = it.nextDecimal(mathContext)
        if (p < lowerDivTotal)
            minBD.add(
                p.multiply(lowerTimesTotal, mathContext)
                    .sqrt(mathContext), mathContext)
        else
            maxBD.subtract(
                BigDecimal.ONE.subtract(p)
                    .multiply(upperTimesTotal, mathContext)
                    .sqrt(mathContext))
    }
}

/**
 * see also:
 * [Wikipedia](https://www.wikiwand.com/en/Categorical_distribution) and
 * [Wolfram](https://www.wolframalpha.com/input/?i=bernoulli+distribution)
 *
 * @param V the type of value to draw
 * @param pmf the probability mass function mapping all possible observations to their probabilities
 * @return a categorical or Multi-noulli [ProbabilityDistribution] sampling values of type [V] using this [PseudoRandom]
 */
fun <V: Any> PseudoRandom.categorical(pmf: Iterable<Pair<V, BigDecimal>>): ProbabilityDistribution<V> {
    // determine sum of weights
    val sum = AtomicReference(BigDecimal.ZERO)
    val map = pmf
            .filter { (value, weight) ->
                val sig = weight.signum()
                require(sig >= 0) { "Negative weight not allowed, value: $value" }
                if(sig == 0)
                    return@filter false
                sum.updateAndGet { s -> s.add(weight) }
                true
            }
            .toMap(mutableMapOf())

    require(map.isNotEmpty()) { "empty" }
    require(sum.get() != BigDecimal.ZERO) { "Zero total probability, values: $map" }
    if (map.size == 1)
        return deterministic(map.keys.iterator().next())

    // normalize and cumulate weights (probability mass) to double precision floats
    val values = arrayOfNulls<Any>(map.size)
    val cumulativeWeights = DoubleArray(map.size)
    val index = AtomicInteger()
    map.forEach { (v_i: V, w_i: BigDecimal) ->
        val i = index.getAndIncrement()
        values[i] = v_i
        val w: Double = w_i.divideBy(sum.get()).toDouble()
        cumulativeWeights[i] = if (i == 0) w else w + cumulativeWeights[i - 1]
    }
    return this.toDistribution {
        val i = Arrays.binarySearch(cumulativeWeights, it.nextDouble())
        @Suppress("UNCHECKED_CAST")
        (if (i < 0) values[-i - 1] else values[i]) as V
    }
}

/**
 * see also [wikipedia](https://www.wikiwand.com/en/Empirical_distribution_function)
 * @return an empirical [ProbabilityDistribution] drawing 64-bit/[Double]-precision floating-point values using this [PseudoRandom]
 */
fun <V: Number> PseudoRandom.empirical(
    observations: Stream<V>,
    binCount: Int = 1
): ProbabilityDistribution<Double> {
    require(binCount > 0) { "non-positive binCount: $binCount" }

    // FIXME where's the Kotlin equivalent?
    val counts: TreeMap<BigDecimal, Long> = observations.collect(
            Collectors.groupingBy(Number::toDecimal, ::TreeMap, Collectors.counting()))

    require(counts.size >= binCount) { "Not enough observations per bin: ${counts.size} < $binCount" }

    val first: BigDecimal = counts.firstKey()
    val binSize: BigDecimal = counts.lastKey().subtract(first).divideBy(binCount)
    val bins: List<Range<BigDecimal>> = (1 until binCount)
            .map { i ->
                Range(
                        binSize.multiplyBy(i - 1), true,
                        binSize.multiplyBy(i), i == binCount)
            }
            .toCollection(mutableListOf())

    val binDist: ConditionalDistribution<BigDecimal, Range<BigDecimal>> = ConditionalDistribution
            .of(param1Gen = { bin -> bin.applyTo(counts) }) { pmf: NavigableMap<BigDecimal, Long> ->
                // FIXME use a Gaussian per bin, i.e. normal( bin_mean, bin_stdev ), given enough (>20) entries per bin
                this.categorical(pmf.entries.map { (v, w) -> Pair(v, w.toBigDecimal()) })
            }
    return this.toDistribution { binDist.draw(it.nextElement(bins)).toDouble() }
}

/** parse a (categorical) [ProbabilityDistribution] based on given valueType and parser */
@Throws(ParseException::class)
fun <T: Any> CharSequence.parseValueDistribution(
    rng: PseudoRandom,
    argParser: (CharSequence) -> Any = { it.parseQuantity() }
): ProbabilityDistribution<T> =
    ProbabilityDistributionType.parseProbabilityDistribution(this, rng, argParser)

/** parse a (categorical) [ProbabilityDistribution] based on given [Enum] [valueType] */
@Throws(ParseException::class)
fun <E: Enum<E>> CharSequence.parseEnumDistribution(rng: PseudoRandom, valueType: Class<E>): ProbabilityDistribution<E> =
    ProbabilityDistributionType.parseProbabilityDistribution(this, rng, { csq ->
        try {
            valueType.enumConstants.first { csq.contentEquals(it.name, true) }
        } catch (e: Exception) {
            throw ParseException("Unable to parse `$csq` as an enum value of: $valueType", length)
        }
    })
    { distArgs ->
        distArgs.ifEmpty { valueType.enumConstants.map { Pair(it, BigDecimal.ONE) } }
    }

@Suppress("UNCHECKED_CAST")
@Throws(ParseException::class)
fun <Q: Quantity<Q>> CharSequence.parseQuantityDistribution(
    rng: PseudoRandom,
    unit: Unit<Q>? = null
): QuantityDistribution<Q> =
    AtomicReference<Unit<Q>>(unit).let { unitHolder ->
        ProbabilityDistributionType.parseProbabilityDistribution<Any, ComparableQuantity<Q>>(this, rng, { it.parseQuantity() })
        { args ->
            // normalize arguments into given or first unit
            if(unitHolder.get() == null)
                unitHolder.set((args[0].first as Quantity<Q>).unit)
            args.map { wv -> Pair((wv.first as Quantity<Q>).toUnit(unitHolder.get()), wv.second) }
        }.let { dist ->
            //
            unitHolder.get()?.let { dist.toQuantities(it) } ?: quantityDistribution { dist.draw() }
        }
    }

internal fun Pair<Any, *>.firstAsNumber(): Number =
    if (this.first is Number)
        this.first as Number
    else if(this.first is Quantity<*>) {
        val qty = (this.first as Quantity<*>)
        if(qty.unit.isCompatible(PURE))
            qty.asType(Dimensionless::class.java).toUnit(PURE).value
        else
            qty.value
    } else
        error("Unable to convert ${this.first::class.java} to a number")
