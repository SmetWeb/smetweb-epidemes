package io.smetweb.math

import org.ejml.simple.SimpleMatrix
import org.ujmp.core.DenseMatrix
import org.ujmp.core.Matrix
import org.ujmp.core.SparseMatrix
import org.ujmp.core.enums.ValueType

//val mat: SimpleMatrix =

private fun verifySize(vararg size: Long): LongArray {
    require(size.isNotEmpty()) { "Provide at least 1 dimension" }
    return if (size.size == 1) longArrayOf(size[0], 1) else size
}

fun zeros(valueType: ValueType = ValueType.DOUBLE, vararg size: Long): DenseMatrix =
        Matrix.Factory.zeros(valueType, *verifySize(*size))

fun sparse(valueType: ValueType = ValueType.BIGDECIMAL, vararg size: Long): SparseMatrix =
        Matrix.Factory.sparse(valueType, *verifySize(*size))

fun scalar(value: Any): Matrix =
        when(value) {
            is CharSequence, is CharArray -> Matrix.Factory.linkToValue(value.toString())
            is Int -> Matrix.Factory.linkToValue(value)
            is Long -> Matrix.Factory.linkToValue(value)
            is Float -> Matrix.Factory.linkToValue(value)
            is Double -> Matrix.Factory.linkToValue(value)
            is Boolean -> Matrix.Factory.linkToValue(value)
            is Char -> Matrix.Factory.linkToValue(value)
            is Short -> Matrix.Factory.linkToValue(value)
            is Byte -> Matrix.Factory.linkToValue(value)
            else -> Matrix.Factory.linkToValue(value)
        }
