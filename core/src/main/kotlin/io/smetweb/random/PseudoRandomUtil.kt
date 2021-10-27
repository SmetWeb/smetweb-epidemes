package io.smetweb.random

import kotlin.reflect.KFunction

/** @return a [PseudoRandom] wrapping a [java.util.Random] from the `java.util` package */
fun java.util.Random.toPseudoRandom() = object: PseudoRandom {
    override val seed get() = error("can't access seed of ${this@toPseudoRandom}")
    override fun nextBoolean(): Boolean = this@toPseudoRandom.nextBoolean()
    override fun nextBytes(bytes: ByteArray): ByteArray = bytes.apply { this@toPseudoRandom.nextBytes(this) }
    override fun nextInt(): Int = this@toPseudoRandom.nextInt()
    override fun nextIntBelow(boundIncl: Int): Int = this@toPseudoRandom.nextInt(boundIncl)
    override fun nextLong(): Long = this@toPseudoRandom.nextLong()
    override fun nextFloat(): Float = this@toPseudoRandom.nextFloat()
    override fun nextDouble(): Double = this@toPseudoRandom.nextDouble()
    override fun nextGaussian(): Double = this@toPseudoRandom.nextGaussian()
}

/** @return a [PseudoRandom] wrapping a [kotlin.random.Random] from the `kotlin.random` package */
fun kotlin.random.Random.toPseudoRandom() = object: PseudoRandom {
    override val seed get() = error("can't access seed of ${this@toPseudoRandom}")
    override fun nextBoolean(): Boolean = this@toPseudoRandom.nextBoolean()
    override fun nextBytes(bytes: ByteArray) = this@toPseudoRandom.nextBytes(bytes)
    override fun nextInt(): Int = this@toPseudoRandom.nextInt()
    override fun nextIntBelow(boundIncl: Int): Int = this@toPseudoRandom.nextInt(boundIncl)
    override fun nextLong(): Long = this@toPseudoRandom.nextLong()
    override fun nextLongBelow(boundIncl: Long): Long = this@toPseudoRandom.nextLong(boundIncl)
    override fun nextFloat(): Float = this@toPseudoRandom.nextFloat()
    override fun nextDouble(): Double = this@toPseudoRandom.nextDouble()
}

fun KFunction<Int>.fillBytes(bytes: ByteArray): ByteArray {
    var i = 0
    val len: Int = bytes.size
    while (i < len) {
        var rnd = this.call()
        var n = (len - i).coerceAtMost(Int.SIZE_BYTES)
        while (n-- > 0) {
            bytes[i++] = rnd.toByte()
            rnd = rnd shr Byte.SIZE_BITS
        }
    }
    return bytes
}

fun KFunction<Int>.coerceBelow(bound: Int): Int {
    require(bound > 0) { "bound <= 0" }

    // skip 2^n matching, as per http://stackoverflow.com/a/2546186
    var bits: Int
    var result: Int
    do {
        bits = this.call() shl 1 ushr 1
        result = bits % bound
    } while (bits - result + (bound - 1) < 0)
    return result
}

fun KFunction<Long>.coerceBelow(bound: Long): Long {
    require(bound > 0) { "bound <= 0" }

    // skip 2^n matching, as per http://stackoverflow.com/a/2546186
    var bits: Long
    var result: Long
    do {
        bits = this.call() shl 1 ushr 1
        result = bits % bound
    } while (bits - result + (bound - 1) < 0)
    return result
}

private var nextNextGaussian = 0.0
private var haveNextNextGaussian = false

/**
 * @see java.util.Random.nextGaussian
 * @see StrictMath.sqrt
 * @see StrictMath.log
 */
@Synchronized
fun KFunction<Double>.nextGaussian(): Double {
    // See Knuth, ACP, Section 3.4.1 Algorithm C.
    return if (haveNextNextGaussian) {
        haveNextNextGaussian = false
        nextNextGaussian
    } else {
        var v1: Double
        var v2: Double
        var s: Double
        do {
            v1 = 2 * this.call() - 1 // between -1 and 1
            v2 = 2 * this.call() - 1 // between -1 and 1
            s = v1 * v1 + v2 * v2
        } while (s >= 1 || s == 0.0)
        val multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s)
        nextNextGaussian = v2 * multiplier
        haveNextNextGaussian = true
        v1 * multiplier
    }
}