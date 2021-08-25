package io.smetweb.math

import org.ujmp.core.DenseMatrix
import org.ujmp.core.DenseMatrix2D
import org.ujmp.core.Matrix
import org.ujmp.core.SparseMatrix
import org.ujmp.core.enums.ValueType
import org.ujmp.core.objectmatrix.SparseObjectMatrix2D

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

    fun <M: Matrix> build(matrix: M): M = matrix.let {
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

    fun empty(): DenseMatrix {
        val matrix = Matrix.Factory.emptyMatrix()
        return build(matrix) as DenseMatrix
    }
}

// MatrixDsl
fun buildEmptyMatrix(init: MatrixBuilder.() -> Unit): DenseMatrix =
    MatrixBuilder().let {
        it.init()
        it.build(Matrix.Factory.emptyMatrix())
    }

fun buildZeroMatrix(init: MatrixBuilder.() -> Unit): DenseMatrix =
    MatrixBuilder().let {
        it.init()
        it.build(Matrix.Factory.zeros(*it.size))
    }

fun buildEyeMatrix(init: MatrixBuilder.() -> Unit): DenseMatrix =
    MatrixBuilder().let {
        it.init()
        it.build(Matrix.Factory.eye(*it.size))
    }

fun buildScalarMatrix(value: Any, init: MatrixBuilder.() -> Unit): DenseMatrix2D =
    MatrixBuilder().let {
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
        }.apply {
            it.init()
            it.build(this)
        }
    }

fun buildSparseMatrix(init: MatrixBuilder.() -> Unit): SparseMatrix =
    MatrixBuilder().let {
        it.init()
        it.build(Matrix.Factory.sparse(it.valueType, *it.size))
    }

fun buildSparseObjectMatrix2D(init: MatrixBuilder.() -> Unit): SparseObjectMatrix2D =
    MatrixBuilder().let {
        it.init()
        it.build(SparseObjectMatrix2D.Factory.zeros(*it.size))
    }

