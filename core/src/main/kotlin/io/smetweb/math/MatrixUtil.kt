package io.smetweb.math

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.kotlin.toFlowable
import kotlinx.coroutines.flow.*
import org.ujmp.core.Matrix
import org.ujmp.core.SparseMatrix
import org.ujmp.core.bigdecimalmatrix.impl.DefaultSparseBigDecimalMatrix
import org.ujmp.core.calculation.Calculation.Ret
import org.ujmp.core.enums.ValueType
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Stream
import java.util.stream.StreamSupport

const val PARALLEL = true

/**
 * FIXME work-around, see [UJMP issue #22](https://github.com/ujmp/universal-java-matrix-package/issues/22)
 *
 * @param source the matrix to traverse
 * @param all whether to return all coordinates, or just the sparse ones
 * @return a [Iterable] coordinate supplier for parallel traversal
 */
fun Matrix.coordinates2D(all: Boolean = false, source: Matrix = this): Iterable<LongArray> {
    if(!all && this is SparseMatrix)
        return availableCoordinates() // beware of unsafe writing in the underlying HashMap of sparse matrices!

    // thread-safe iterator for DenseMatrix type
    return Iterable {
        object : Iterator<LongArray> {

            private var cols: Long = source.size[1]
            private var cells: Long = cols * source.size[0]
            private var current = AtomicLong(0L)

            override fun hasNext(): Boolean =
                current.get() < cells

            override fun next(): LongArray {
                val i = current.getAndIncrement()
                val row = i / cols
                val col = i % cols
                return longArrayOf(row, col)
            }
        }
    }
}

fun Matrix.coordinatesStream(
    all: Boolean = false,
    parallel: Boolean = PARALLEL
): Stream<LongArray> {
    val coords = coordinates2D(all = all)
    val spliterator: Spliterator<LongArray> =
        Spliterators.spliterator(coords.iterator(), rowCount * columnCount, Spliterator.SIZED xor Spliterator.SUBSIZED)
    return StreamSupport.stream(spliterator, parallel)
}

fun Matrix.coordinatesFlow(
    all: Boolean = false,
    parallel: Boolean = PARALLEL
): Flow<LongArray> {
    val coordinateFlow = coordinates2D(all = all).asFlow()
    return when (parallel) {
        true -> coordinateFlow.buffer()
        else -> coordinateFlow
    }
}

fun Matrix.coordinatesFlowable(
    all: Boolean = false
): Flowable<LongArray> = coordinates2D(all = all).toFlowable()

fun <M: Matrix> M.forEach(
    all: Boolean = false,
    parallel: Boolean = PARALLEL,
    getter: (LongArray) -> Any? = { x -> getAsObject(*x) },
    visitor: (LongArray, Any?) -> Unit
): M {
    coordinatesStream(all = all, parallel = parallel)
        .forEach { x: LongArray ->
            visitor(x, getter(x))
        }
    return this
}

@Suppress("UNCHECKED_CAST")
fun <M: Matrix, T: Any?> M.compute(
    all: Boolean = true,
    parallel: Boolean = PARALLEL,
    getter: (LongArray) -> T = { x -> getAsObject(*x) as T },
    setter: (T, LongArray) -> Unit =
        if (valueType == ValueType.BIGDECIMAL && this is DefaultSparseBigDecimalMatrix)
        // FIXME workaround: DefaultSparseBigDecimalMatrix does not override AbstractMatrix.setAsBigDecimal,
        //  which stores ValueType.BIGDECIMAL as double
            { v, x -> setBigDecimal(v as BigDecimal, *x) }
        else
            ::setAsObject,
    vararg coords: Long,
    transform: (T, LongArray) -> T,
): M {
    if (coords.isEmpty()) {
        val stream = coordinatesStream(all = all, parallel = parallel)
        if (parallel && this is SparseMatrix) {
            // Sparse matrices with underlying HashMap are not thread-safe, must synchronize parallel write-ops
            stream.forEach { x: LongArray ->
                val result = transform(getter(x), x)
                synchronized(this) {
                    setter(result, x)
                }
            }
        } else {
            // serial operations always thread-safe, as well as parallel write-ops on array-based matrices
            stream.forEach { x: LongArray ->
                setter(transform(getter(x), x), x)
            }
        }
    } else {
        setter( transform( getter(coords), coords), coords)
    }
    return this
}

fun Matrix.sum(parallel: Boolean = PARALLEL): BigDecimal =
    coordinatesStream(all = false, parallel = parallel)
        .map { getAsBigDecimal(*it) }
        .reduce(BigDecimal::add)
        .orElse(BigDecimal.ZERO)
        .stripTrailingZeros()

fun <T: Matrix> T.add(augend: Number, vararg coords: Long): T =
    compute(coords = coords) { value: Any?, _ ->
        (value as Number?)?.add(augend) ?: augend
    }

fun <M: Matrix> M.add(that: Matrix, parallel: Boolean = PARALLEL): M =
    compute(parallel = parallel) { value: Any?, coords: LongArray ->
        val augend = that.getAsObject(*coords) as Number
        when(value) {
            is Number -> value.add(augend)
            else -> augend
        }
    }

fun <M: Matrix> M.subtract(subtrahend: Number, vararg coords: Long): M =
    add(augend = subtrahend.opposite(), coords = coords)

fun <T: Matrix> T.subtract(that: Matrix, parallel: Boolean = PARALLEL): T =
    compute(parallel = parallel) { value: Any?, coords: LongArray ->
        val subtrahend = that.getAsObject(*coords) as Number
        (value as Number?)?.subtract(subtrahend) ?: subtrahend.opposite()
    }

fun <T: Matrix> T.multiply(multiplicand: Number, vararg coords: Long): T =
    compute(coords = coords) { value: Any?, _ ->
        (value as Number?)?.multiplyBy(multiplicand) ?: BigDecimal.ZERO
    }

fun Matrix.get(vararg coords: Long): BigDecimal =
    getAsBigDecimal(*coords)

/** offset used for conversion of Enum ordinals, to avoid `zero` values which are stored as `null` */
private const val ENUM_ORDINAL_OFFSET = 1

@Suppress("UNCHECKED_CAST")
fun <T> Matrix.getNumericOrEnum(returnType: Class<T>, vararg coords: Long): T? {
    val value: Any = getAsObject(*coords)
        ?: return when (returnType) {
            // Matrix stores ZERO or FALSE as (Number)0 or (Object)null
            BigDecimal::class.java -> BigDecimal.ZERO
            Double::class.java, java.lang.Double::class.java -> java.lang.Double.valueOf(0.0)
            Float::class.java, java.lang.Float::class.java -> java.lang.Float.valueOf(0f)
            Long::class.java, java.lang.Long::class.java -> java.lang.Long.valueOf(0L)
            Int::class.java, java.lang.Integer::class.java -> Integer.valueOf(0)
            Byte::class.java, java.lang.Byte::class.java -> java.lang.Byte.valueOf(0x0)
            Boolean::class.java, java.lang.Boolean::class.java -> false
            Char::class.java, java.lang.Character::class.java -> Character.MIN_VALUE
            BigInteger::class.java -> BigInteger.ZERO
            else -> null
        } as T?

    val valueType: Class<*> = value.javaClass
    if (returnType.isAssignableFrom(valueType))
        return value as T

    // check/convert number type values if necessary
    if (value is Number) {
        val decimalValue: BigDecimal = value.toDecimal()
        return if (returnType.isEnum)
            returnType.enumConstants[decimalValue.toInt() - ENUM_ORDINAL_OFFSET]
        else when (returnType) {
            BigDecimal::class.java -> decimalValue
            Double::class.java, java.lang.Double::class.java -> java.lang.Double.valueOf(decimalValue.toDouble())
            Float::class.java, java.lang.Float::class.java -> java.lang.Float.valueOf(decimalValue.toFloat())
            Long::class.java, java.lang.Long::class.java -> java.lang.Long.valueOf(decimalValue.longValueExact())
            Int::class.java, java.lang.Integer::class.java -> Integer.valueOf(decimalValue.intValueExact())
            Byte::class.java, java.lang.Byte::class.java -> java.lang.Byte.valueOf(decimalValue.byteValueExact())
            Boolean::class.java, java.lang.Boolean::class.java -> java.lang.Boolean.valueOf(decimalValue.signum() == 0)
            Char::class.java, java.lang.Character::class.java -> Character.valueOf(decimalValue.toChar())
            BigInteger::class.java -> decimalValue.toBigInteger()
            else -> error("Expected $returnType, got $valueType")
        } as T
    }
    error ("Unable to restore $returnType from (non-numeric) type: $valueType")
}

fun Matrix.putNumericOrEnum(value: Any?, vararg coords: Long) {
    val number: Number = when (value?.javaClass?.isEnum) {
        true -> ENUM_ORDINAL_OFFSET + (value as Enum<*>).ordinal
        false, null -> value as Number
    }
    setAsObject(number, *coords)
}

fun <T: Matrix> T.put(value: Number, vararg coords: Long): T {
    val decimal = value.toDecimal()
    setAsBigDecimal(decimal, *coords)
    return this
}

fun <T: Matrix> T.put(value: Any, vararg coords: Long): T {
    setAsObject(value, *coords)
    return this
}

fun <T: Matrix, V: Any> T.put(values: Stream<V>, vararg offset: Long): T {
    val i = AtomicReference(longArrayOf(*offset))
    values.forEach { value: V ->
        val coords = i.getAndUpdate { x: LongArray ->
            when (x.size) {
                1 -> longArrayOf(x[0] + 1)
                2 -> longArrayOf(x[0], x[1] + 1)
                else -> longArrayOf(1)
            }
        }
        put(value, *coords)
    }
    return this
}

fun <T: Matrix, E: Enum<E>, V: Number> T.put(values: Map<E, V>, vararg offset: Long): T {
    val i = AtomicReference(when (offset.isEmpty()) {
        false -> longArrayOf(*offset)
        else -> (1..dimensionCount).map { 0L }.toLongArray()
    })
    values.forEach { (key: E, value: V) ->
        val coords = i.getAndUpdate { x: LongArray ->
            when (dimensionCount) {
                1 -> longArrayOf(x[0] + 1)
                2 -> longArrayOf(x[0], x[1] + 1)
                else -> longArrayOf(1)
            }
        }
        when (dimensionCount) {
            1 -> setRowLabel(key.ordinal.toLong(), key.name)
            else -> setColumnLabel(key.ordinal.toLong(), key.name)
        }
        setAsBigDecimal(value.toDecimal(), *coords)
    }
    return this
}

fun <T: Matrix> T.put(values: Matrix, vararg offset: Long): T {
    val x: LongArray = verifyBounds(coords = offset, orOrigin = true)!!
    require(!(x[0] + values.rowCount > size[0]
            || x[1] + values.columnCount > size[1])) {
        ("Does not fit at offset: " + values.size.contentToString() + " + "
                + x.contentToString() + " >= " + size.contentToString())
    }
    setContent(Ret.ORIG, values, *x)
    return this
}

fun Matrix.verifyBounds(vararg coords: Long, orOrigin: Boolean = false): LongArray? {
    val dimCount = dimensionCount

    // assume null -> origin (eg. scalar)
    if (coords.isEmpty())
        return if (orOrigin)
            LongArray(dimensionCount)
        else
            null
    if (coords.size != dimensionCount) {
        if (coords.size == 1) // convert vector: 1D -> 2D or multiD
        {
            return if (isRowVector)
            // try vertical
                verifyBounds(coords[0], 0)
            else if (isColumnVector)
            // try horizontal
                verifyBounds(0, coords[0])
            else {
                // try diagonal
                val diagonal = LongArray(dimensionCount)
                Arrays.fill(diagonal, coords[0])
                verifyBounds(*diagonal)
            }
        }
        throw IndexOutOfBoundsException(
            "Dimensions ${coords.contentToString()}.length <> $dimCount")
    }
    for (dim in 0 until dimensionCount)
        if (coords[dim] >= size[dim])
            throw IndexOutOfBoundsException(("Coordinates " + coords.contentToString()
                    + " out of bounds: " + Arrays.toString(size)))
    return coords
}