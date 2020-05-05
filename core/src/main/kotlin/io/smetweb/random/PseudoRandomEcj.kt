package io.smetweb.random

import java.io.Serializable

/**
 * [PseudoRandomEcj] is a simplified port the `MersenneTwisterFast` implementation
 * by Sean Luke, distributed in the [ECJ library](https://github.com/GMUEClab/ecj)
 */
@Strictfp
class PseudoRandomEcj(
        override val seed: Long = System.currentTimeMillis()
): Serializable, PseudoRandom {

    private var mt: IntArray = intArrayOf() // the array for the state vector
    private var mti = 0 // mti==N+1 means mt[N] is not initialized
    private var mag01: IntArray = intArrayOf()

    // a good initial seed (of int size, though stored in a long)
    //private static final long GOOD_SEED = 4357;
    private var nextNextGaussian = 0.0
    private var haveNextNextGaussian = false

    /**
     * Constructor using a given seed.  Though you pass this seed in
     * as a long, it's best to make sure it's actually an integer.
     *
     */
    init {
        setSeed(this.seed)
    }

    /**
     * Constructor using an array of integers as seed.
     * Your array must have a non-zero length.  Only the first 624 integers
     * in the array are used; if the array is shorter than this then
     * integers are repeatedly used in a wrap-around fashion.
     */
//    constructor(array: IntArray) {
//        setSeed(array)
//    }

    /** Returns true if the MersenneTwisterFast's current internal state is equal to another MersenneTwisterFast.
     * This is roughly the same as equals(other), except that it compares based on value but does not
     * guarantee the contract of immutability (obviously random number generators are immutable).
     * Note that this does NOT check to see if the internal gaussian storage is the same
     * for both.  You can guarantee that the internal gaussian storage is the same (and so the
     * nextGaussian() methods will return the same values) by calling clearGaussian() on both
     * objects.  */
    fun stateEquals(other: PseudoRandomEcj?): Boolean {
        if (other === this) return true
        if (other == null) return false
        if (mti != other.mti) return false
        for (x in mag01.indices) if (mag01[x] != other.mag01[x]) return false
        for (x in mt.indices) if (mt[x] != other.mt[x]) return false
        return true
    }

    /**
     * Initalize the pseudo random number generator.  Don't
     * pass in a long that's bigger than an int (Mersenne Twister
     * only uses the first 32 bits for its seed).
     */
    fun setSeed(seed: Long) {
        // Due to a bug in java.util.Random clear up to 1.2, we're
        // doing our own Gaussian variable.
        haveNextNextGaussian = false
        mt = IntArray(N)
        mag01 = IntArray(2)
        mag01[0] = 0x0
        mag01[1] = MATRIX_A
        mt[0] = (seed and -0x1).toInt()
        mti = 1
        while (mti < N) {
            mt[mti] = 1812433253 * (mt[mti - 1] xor (mt[mti - 1] ushr 30)) + mti
            mti++
        }
    }

    /**
     * Sets the seed of the MersenneTwister using an array of integers.
     * Your array must have a non-zero length.  Only the first 624 integers
     * in the array are used; if the array is shorter than this then
     * integers are repeatedly used in a wrap-around fashion.
     */
    fun setSeed(array: IntArray) {
        require(array.isNotEmpty()) { "Array length must be greater than zero" }
        var i: Int
        var j: Int
        setSeed(19650218)
        i = 1
        j = 0
        var k: Int = if (N > array.size) N else array.size
        while (k != 0) {
            mt[i] = (mt[i] xor (mt[i - 1] xor (mt[i - 1] ushr 30)) * 1664525) + array[j] + j /* non linear */
            // mt[i] &= 0xffffffff; /* for WORDSIZE > 32 machines */
            i++
            j++
            if (i >= N) {
                mt[0] = mt[N - 1]
                i = 1
            }
            if (j >= array.size) j = 0
            k--
        }
        k = N - 1
        while (k != 0) {
            mt[i] = (mt[i] xor (mt[i - 1] xor (mt[i - 1] ushr 30)) * 1566083941) - i /* non linear */
            // mt[i] &= 0xffffffff; /* for WORDSIZE > 32 machines */
            i++
            if (i >= N) {
                mt[0] = mt[N - 1]
                i = 1
            }
            k--
        }
        mt[0] = -0x80000000 /* MSB is 1; assuring non-zero initial array */
    }

    override fun nextInt(): Int {
        var y: Int
        if (mti >= N) // generate N words at one time
        {
            val mt = mt // locals are slightly faster
            val mag01 = mag01 // locals are slightly faster
            var kk: Int = 0
            while (kk < N - M) {
                y = mt[kk] and UPPER_MASK or (mt[kk + 1] and LOWER_MASK)
                mt[kk] = mt[kk + M] xor (y ushr 1) xor mag01[y and 0x1]
                kk++
            }
            while (kk < N - 1) {
                y = mt[kk] and UPPER_MASK or (mt[kk + 1] and LOWER_MASK)
                mt[kk] = mt[kk + (M - N)] xor (y ushr 1) xor mag01[y and 0x1]
                kk++
            }
            y = mt[N - 1] and UPPER_MASK or (mt[0] and LOWER_MASK)
            mt[N - 1] = mt[M - 1] xor (y ushr 1) xor mag01[y and 0x1]
            mti = 0
        }
        y = mt[mti++]
        y = y xor (y ushr 11) // TEMPERING_SHIFT_U(y)
        y = y xor (y shl 7 and TEMPERING_MASK_B) // TEMPERING_SHIFT_S(y)
        y = y xor (y shl 15 and TEMPERING_MASK_C) // TEMPERING_SHIFT_T(y)
        y = y xor (y ushr 18) // TEMPERING_SHIFT_L(y)
        return y
    }

    fun nextShort(): Short {
        val y: Int = nextInt()
        return (y ushr 16).toShort()
    }

    override fun nextBoolean(): Boolean {
        val y: Int = nextInt()
        return (y ushr 31 != 0)
    }

    override fun nextBytes(bytes: ByteArray): ByteArray {
        for (x in bytes.indices) {
            val y: Int = nextInt()
            bytes[x] = (y ushr 24).toByte()
        }
        return bytes
    }

    /** Returns a long drawn uniformly from 0 to n-1.  Suffice it to say,
     * n must be greater than 0, or an IllegalArgumentException is raised.  */
    override fun nextLong(): Long {
        val y: Int = nextInt()
        val z: Int = nextInt()
        return (y.toLong() shl Int.SIZE_BITS) + z.toLong()
    }

    /** Returns a long drawn uniformly from 0 to n-1.  Suffice it to say,
     * n must be &gt; 0, or an IllegalArgumentException is raised.  */
    override fun nextLongBelow(bound: Long): Long {
        require(bound > 0) { "n must be positive, got: $bound" }
        var result: Long
        do {
            val bits = nextLong() ushr 1
            result = bits % bound
        } while (bits - result + (bound - 1) < 0)
        return result
    }

    /** Returns a random double in the half-open range from [0.0,1.0).  Thus 0.0 is a valid
     * result but 1.0 is not.  */
    override fun nextDouble(): Double {
        val y: Int = nextInt()
        val z: Int = nextInt()

        /* derived from nextDouble documentation in jdk 1.2 docs, see top */
        return (((y ushr 6).toLong() shl 27) + (z ushr 5)) / (1L shl 53).toDouble()
    }

    /**
     * Clears the internal gaussian variable from the RNG.  You only need to do this
     * in the rare case that you need to guarantee that two RNGs have identical internal
     * state.  Otherwise, disregard this method.  See stateEquals(other).
     */
    fun clearGaussian() {
        haveNextNextGaussian = false
    }

    override fun nextGaussian(): Double {
        return if (haveNextNextGaussian) {
            haveNextNextGaussian = false
            nextNextGaussian
        } else {
            var v1: Double
            var v2: Double
            var s: Double
            do {
                val y: Int = nextInt()
                val z: Int = nextInt()
                val a: Int = nextInt()
                val b: Int = nextInt()

                /* derived from nextDouble documentation in jdk 1.2 docs, see top */
                v1 = (2 * ((((y ushr 6).toLong() shl 27) + (z ushr 5)) / (1L shl 53).toDouble()) - 1)
                v2 = (2 * ((((a ushr 6).toLong() shl 27) + (b ushr 5)) / (1L shl 53).toDouble()) - 1)
                s = v1 * v1 + v2 * v2
            } while (s >= 1 || s == 0.0)
            val multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s)
            nextNextGaussian = v2 * multiplier
            haveNextNextGaussian = true
            v1 * multiplier
        }
    }

    /** Returns a random float in the half-open range from [0.0f,1.0f).  Thus 0.0f is a valid
     * result but 1.0f is not.  */
    override fun nextFloat(): Float {
        val y: Int = nextInt()
        return (y ushr 8) / (1 shl 24).toFloat()
    }

    /** Returns an integer drawn uniformly from 0 to n-1.  Suffice it to say,
     * n must be &gt; 0, or an IllegalArgumentException is raised.  */
    override fun nextIntBelow(bound: Int): Int {
        require(bound > 0) { "n must be positive, got: $bound" }
        if (bound and -bound == bound) // i.e., n is a power of 2
        {
            val y: Int = nextInt()
            return (bound * (y ushr 1).toLong() shr 31).toInt()
        }
        var bits: Int
        var result: Int
        do {
            val y: Int = nextInt()
            bits = y ushr 1
            result = bits % bound
        } while (bits - result + (bound - 1) < 0)
        return result
    }

    companion object {
        // Serialization
        private const val serialVersionUID = -8219700664442619525L // locked as of Version 15

        // Period parameters
        private const val N = 624
        private const val M = 397
        private const val MATRIX_A = -0x66f74f21 //    private static final * constant vector a
        private const val UPPER_MASK = -0x80000000 // most significant w-r bits
        private const val LOWER_MASK = 0x7fffffff // least significant r bits

        // Tempering parameters
        private const val TEMPERING_MASK_B = -0x62d3a980
        private const val TEMPERING_MASK_C = -0x103a0000

    }
}
