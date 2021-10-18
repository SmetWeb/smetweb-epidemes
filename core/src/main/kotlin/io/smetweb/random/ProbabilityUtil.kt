package io.smetweb.random

import io.smetweb.math.*
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.toList

fun <T> deterministic(value: T): ProbabilityDistribution<T> =
        object: ProbabilityDistribution<T> {
            override fun draw(): T = value
        }

fun <V> PseudoRandom.toDistribution(transform: (PseudoRandom) -> V): ProbabilityDistribution<V> =
        object: ProbabilityDistribution<V> {
            override fun draw(): V = transform(this@toDistribution)
        }

fun PseudoRandom.bernoulli(probability: Number): ProbabilityDistribution<Boolean> {
    val p = probability.toDouble()
    require(p >= 0) { "Negative probability: $probability" }
    return when(p) {
        0.0 -> deterministic(false)
        1.0 -> deterministic(true)
        else -> this.toDistribution { it.nextDouble() < p }
    }
}

fun <V> PseudoRandom.uniform(values: Iterable<V>): ProbabilityDistribution<V> {
    val list: List<V> = values.toCollection(mutableListOf())
    return when(list.size) {
        0 -> throw IllegalArgumentException("No values")
        1 -> deterministic(list[0])
        else -> this.toDistribution { it.nextElement(list) }
    }
}

@Throws(ArithmeticException::class)
fun PseudoRandom.uniform(min: BigInteger, max: BigInteger): ProbabilityDistribution<BigInteger> {
    require(min >= BigInteger.ZERO) { "min < 0" }
    require(max >= min) { "max = min" }
    val range: Long = max.subtract(min).longValueExact()
    return this.toDistribution { it.nextLongBelow(range).toBigInteger().add(min) }
}

fun PseudoRandom.uniform(min: BigDecimal, max: BigDecimal): ProbabilityDistribution<BigDecimal> {
    require(min >= BigDecimal.ZERO) { "min < 0" }
    require(max >= min) { "max = min" }
    val range: BigDecimal = max.subtract(min)
    return this.toDistribution { it.nextDecimal().multiplyBy(range).add(min) }
}

fun PseudoRandom.uniform(min: Int, max: Int): ProbabilityDistribution<Int> {
    require(min >= 0) { "min < 0" }
    require(max >= min) { "max = min" }
    val range: Int = max - min
    return this.toDistribution { it.nextIntBelow(range) + min }
}

fun PseudoRandom.uniform(min: Long, max: Long): ProbabilityDistribution<Long> {
    require(min >= 0L) { "min < 0" }
    require(max > min) { "max =< min" }
    val range: Long = max - min
    return this.toDistribution { it.nextLongBelow(range) + min }
}

fun PseudoRandom.uniform(min: Float, max: Float): ProbabilityDistribution<Float> {
    require(min >= 0F) { "min < 0" }
    require(max >= min) { "max = min" }
    val range: Float = max - min
    return if (range <= 0)
        throw IllegalArgumentException("range: $min >= $max")
    else this.toDistribution { it.nextFloat() * range + min }
}

fun PseudoRandom.uniform(min: Double, max: Double): ProbabilityDistribution<Double> {
    require(min >= 0.0) { "min < 0" }
    require(max >= min) { "max = min" }
    val range: Double = max - min
    return this.toDistribution { it.nextDouble() * range + min }
}

fun PseudoRandom.gaussian(mean: Number, stDev: Number): ProbabilityDistribution<Double> {
    val meanDouble = mean.toDouble()
    val stDevDouble = stDev.toDouble()
    return this.toDistribution { it.nextGaussian() * stDevDouble + meanDouble }
}

fun PseudoRandom.triangular(min: Number, mode: Number, max: Number): ProbabilityDistribution<BigDecimal> {
    val modeBD: BigDecimal = mode.toDecimal()
    val minBD: BigDecimal = min.toDecimal()
    val maxBD: BigDecimal = max.toDecimal()
    val lower = modeBD.subtract(minBD)
    val upper = maxBD.subtract(modeBD)
    val total = maxBD.subtract(minBD)
    val lowerDivTotal: BigDecimal = lower.divideBy(total)
    val lowerTimesTotal: BigDecimal =lower.multiplyBy(total)
    val upperTimesTotal: BigDecimal = upper.multiplyBy(total)
    return this.toDistribution {
        val p: BigDecimal = it.nextDouble().toDecimal()
        if (p < lowerDivTotal)
            minBD.add(p.multiply(lowerTimesTotal).sqrt())
        else maxBD.subtract(BigDecimal.ONE.subtract(p).multiply(upperTimesTotal).sqrt())
    }
}

fun <V: Any> PseudoRandom.categorical(pmf: Stream<Pair<V, BigDecimal>>): ProbabilityDistribution<V> =
    categorical(pmf.toList())

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
 * @see [ProbabilityDistribution]
 */
fun <V: Number> PseudoRandom.empirical(observations: Stream<V>, binCount: Int = 1): ProbabilityDistribution<Double> {
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

    val dist: ConditionalDistribution<BigDecimal, Range<BigDecimal>> = ConditionalDistribution
            .of(param1Gen = { bin -> bin.applyTo(counts) }) { pmf: NavigableMap<BigDecimal, Long> ->
                // FIXME use Gaussians per bin, i.e. normal( bin_mean, bin_stdev ), given enough (>20) entries per bin
                this.categorical(pmf.entries.map { (v, w) -> Pair(v, w.toBigDecimal()) })
            }
    return this.toDistribution { dist.draw(it.nextElement(bins)).toDouble() }
}
