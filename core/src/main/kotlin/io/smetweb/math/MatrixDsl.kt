package io.smetweb.math

import org.ujmp.core.DenseMatrix
import org.ujmp.core.DenseMatrix2D
import org.ujmp.core.Matrix
import org.ujmp.core.SparseMatrix
import org.ujmp.core.enums.ValueType

@DslMarker
annotation class MatrixDsl

@MatrixDsl
class MatrixBuilder {
    var valueType: ValueType = ValueType.BIGDECIMAL
    var size: LongArray = longArrayOf(1, 1)
    var label: Any? = null
    var dimensionLabels: List<Any> = listOf()
    var rowLabeler: (Long) -> Any = { }
    var columnLabeler: (Long) -> Any = { }
    private var labels: MutableMap<Long, MutableList<Any>> = mutableMapOf()

    /**
     * as per [Wikipedia](https://www.wikiwand.com/en/Matrix_(mathematics)):
     *
     * > a matrix with two rows and three columns; one say often
     * > a "two by three matrix", a "2×3-matrix", or a matrix of dimension 2×3
     */
    infix fun Int.by(target: Long) = longArrayOf(this.toLong(), target)

    /**
     * as per [Wikipedia](https://www.wikiwand.com/en/Matrix_(mathematics)):
     *
     * > a matrix with two rows and three columns; one say often
     * > a "two by three matrix", a "2×3-matrix", or a matrix of dimension 2×3
     */
    infix fun Long.by(target: Long) = longArrayOf(this, target)

    fun sparse(): SparseMatrix {
        val matrix = Matrix.Factory.sparse(valueType, *size)
        return build(matrix) as SparseMatrix
    }

    fun empty(): DenseMatrix {
        val matrix = Matrix.Factory.emptyMatrix()
        return build(matrix) as DenseMatrix
    }

    fun zeros(): DenseMatrix {
        val matrix = Matrix.Factory.zeros(valueType, *size)
        return build(matrix) as DenseMatrix
    }

    fun eye(): DenseMatrix {
        val matrix = Matrix.Factory.eye(*size)
        return build(matrix) as DenseMatrix
    }

    fun scalar(value: Any): DenseMatrix2D {
        val matrix =  when(value) {
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
        return build(matrix) as DenseMatrix2D
    }

    private fun build(matrix: Matrix): Matrix = matrix.let {
        it.setLabel(label)
        dimensionLabels.forEachIndexed(it::setDimensionLabel)
        (0..it.rowCount).forEach { i -> it.setRowLabel(i, rowLabeler(i)) }
        (0..it.columnCount).forEach { i -> it.setColumnLabel(i, columnLabeler(i)) }
        when {
            it.isRowVector -> labels
                .forEach { (i, labels) -> it.setRowLabel(i, labels[0]) }
            it.isColumnVector -> labels
                .forEach { (i, labels) -> it.setColumnLabel(i, labels[0]) }
            else -> labels
                .forEach { (i, labels) ->
                    if(i < it.rowCount)
                        it.setRowLabel(i, labels[0])
                    if(i < it.columnCount)
                        it.setColumnLabel(i, when (labels.size) { 1 -> labels[0] else -> labels[1] })
                }
        }
        it
    }

    fun label(i: Long, vararg label: Any = emptyArray()) {
        if(label.isNotEmpty())
            labels[i] = label.toMutableList()
    }

    fun labelRows(vararg rowLabels: Any) {
        rowLabels.forEachIndexed { i, label ->
            labels.compute(i.toLong()) { _, v ->
                v?.apply {
                    this[0] = label
                } ?: mutableListOf(label)
            }
        }
    }

    fun labelColumns(vararg colLabels: Any) {
        colLabels.forEachIndexed { i, label ->
            labels.compute(i.toLong()) { _, v ->
                v?.apply {
                    when (this.size) {
                        1 -> this.add(label)
                        else -> this[1] = label
                    }
                } ?: mutableListOf("", label)
            }
        }
    }
}

// MatrixDsl
fun sparseMatrix(init: MatrixBuilder.() -> Unit): SparseMatrix =
    MatrixBuilder().let { builder ->
        builder.init()
        builder.sparse()
    }
