package io.smetweb.math

import io.smetweb.reflect.typeArgumentsFor
import org.ujmp.core.Matrix
import org.ujmp.core.calculation.Calculation
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class MatrixTable(
    properties: List<Class<out Property<*>>>,
    matrix: Matrix,

    colValueTypes: List<Class<*>> =
        properties.map { it.typeArgumentsFor(Property::class.java)[0] },
    rowLabeler: (Long) -> String = { "row-$it" },
    rowMax: AtomicLong = AtomicLong(),
    rowRecycler: SortedSet<Long> = TreeSet(),
    rowValidator: (Long) -> Boolean =
        { pk -> pk > -1 && pk < rowMax.get() && !rowRecycler.contains(pk) },
    indexer: () -> Iterable<Long> = { buildIndex(rowMax.get(), rowRecycler) },
    retriever: (Long) -> Tuple<Long>? =
        { pk ->
            if (rowValidator(pk))
                MatrixTuple(matrix, pk, properties, colValueTypes)
            else
                null
        },
): Table<Long>(
    counter = { rowMax.get() - rowRecycler.size },
    printer = { // matrix::toString,
        buildString {
            val label = matrix.label.colValueToString { matrix::class.java.simpleName }
            append((colValueTypes.indices).joinToString(separator = Tuple.delim, prefix = "\n$label ") { col ->
                matrix.getColumnLabel(col.toLong()).colValueToString { "Col$col" }
            })
            append(indexer().joinToString(separator = ",\n", prefix = "\n") { row ->
                rowVectorToString(matrix.getRowLabel(row), matrix.selectRows(Calculation.Ret.LINK, row), colValueTypes)
            })
        }
    },
    cleaner = matrix::clear,
    indexer = indexer,
    inserter = {
        if (rowRecycler.isEmpty()) {
            rowMax.getAndIncrement().apply {
                if (matrix.rowCount == this) { // exceeded row limit
                    if (matrix.isResizable)
                        matrix.setSize(2 * matrix.rowCount, matrix.columnCount)
                    else
                        error("Insufficient rows (${matrix.rowCount}) and not resizable: ${matrix::class.java}")
                }
            }
        } else {
            rowRecycler.first().apply { rowRecycler.remove(this) }
        }.let { row ->
            matrix.setRowLabel(row, rowLabeler(row))
            MatrixTuple(matrix, row, properties, colValueTypes)
        }
    },
    retriever = retriever,
    remover = { row ->
        if (!rowValidator(row))
            error("Key $row not in [0,$rowMax], or marked as removed: $rowRecycler")
        val old = retriever(row)
        matrix.selectRows(Calculation.Ret.LINK, row).clear()
        matrix.setRowLabel(row, null)
        if (row == rowMax.get())
            rowMax.decrementAndGet()
        else
            rowRecycler.add(row)
        old
    },
) {

    init {
        matrix.apply {
            if(columnCount < properties.size)
                error("Insufficient columns ($columnCount)" + " to fit all properties (${properties.size})")
            properties.forEachIndexed { i, propertyType ->
                val col = i.toLong()
                setColumnLabel(col, propertyType.simpleName)
            }
        }
    }

    companion object {

        private const val colWidth = 8

        fun Any?.colValueToString(alt: () -> String = { "?" }) =
            "%${colWidth}s".format(((this?.toString()?.ifEmpty { null }) ?: alt()).let {
                if(it.length > colWidth - 1)
                    it.substring(0, it.length.coerceAtMost(colWidth - 2)) + ".."
                else
                    it.substring(0, it.length.coerceAtMost(colWidth))
            })

        fun rowVectorToString(rowLabel: String, rowVector: Matrix, columnValueTypes: List<Class<*>>) = buildString {
            val valueStr = columnValueTypes
                .mapIndexed { i, type -> rowVector.getNumericOrEnum(type, 0, i.toLong()).colValueToString() }
                .joinToString(Tuple.delim)
            append(rowLabel.colValueToString(), Tuple.start, valueStr, Tuple.end)
        }

        fun buildIndex(rowMax: Long, removedRows: Collection<Long>): Iterable<Long> {
            if (rowMax == 0L)
                return emptyList()
            // map removed indices from `rowRecycler` onto the (non-empty) sequences in between
            val ignore: MutableList<Long> = LinkedList(removedRows)
            ignore.add(rowMax)
            // move first cut-off point
            var initialCutOff: Long = 0
            while (ignore[0] == initialCutOff) {
                ignore.removeAt(0)
                initialCutOff++
            }
            return (0 until ignore.size)
                .map { i -> Pair(if (i == 0) initialCutOff else ignore[i - 1] + 1, ignore[i]) }
                .filter { (l, r) -> l != r } // skip empty sequences
                .flatMap { (l, r) -> l until r }
        }
    }

    private class MatrixTuple(
        private val matrix: Matrix,
        override val key: Long,
        override val properties: List<Class<out Property<*>>>,
        private val colValueTypes: List<Class<*>> =
            properties.map { it.typeArgumentsFor(Property::class.java)[0] },
    ): Table.Tuple<Long>(
        key = key,
        properties = properties,
        getter = { colType ->
            properties.indexOf(colType)
                .takeIf { it >= 0 }
                ?.let { col -> matrix.getNumericOrEnum(colValueTypes[col], key, col.toLong()) }
                ?: error("Invalid property: $colType, allowed: $properties")
        },
        setter = { colType, value ->
            properties.indexOf(colType)
                .takeIf { it >= 0 }
                ?.let { col -> matrix.setAsBigDecimal((value as Number).toDecimal(), key, col.toLong()) }
                ?: error("Invalid property: $colType, allowed: $properties")
        }
    )
}
