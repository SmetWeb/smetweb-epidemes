package io.smetweb.math

import io.smetweb.reflect.typeArgumentsFor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.ujmp.core.Matrix
import org.ujmp.core.calculation.Calculation.Ret
import org.ujmp.core.enums.ValueType
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class TableTest {

    class Prop1(value: Long): Table.Property<Long>(value)

    class HashMapTuple<PK: Any>(
        override val key: PK,
        override val properties: List<Class<out Table.Property<*>>>,
        private val map: MutableMap<Class<out Table.Property<*>>, Any?> = mutableMapOf()
    ): Table.Tuple<PK>(
        key = key,
        properties = properties,
        getter = map::get,
        setter = map::put)

    class HashMapTable<PK: Any>(
        private val properties: List<Class<out Property<*>>>,
        private val keyGen: () -> PK,
        private val map: MutableMap<PK, Tuple<PK>> = mutableMapOf()
    ): Table<PK>(
        counter = { map.size.toLong() },
        printer = map::toString,
        cleaner = map::clear,
        indexer = { map.keys },
        retriever = map::get,
        remover = { map.remove(it) },
        inserter = { HashMapTuple(keyGen(), properties).apply { map[this.key] = this } }
    )

    class MatrixTable(
        private val properties: List<Class<out Property<*>>>,
        private val matrix: Matrix,

        private val colValueTypes: List<Class<*>> =
            properties.map { it.typeArgumentsFor(Property::class.java)[0].second },
        private val rowLabeler: (Long) -> String = { "row-$it" },
        private val rowMax: AtomicLong = AtomicLong(),
        private val rowRecycler: SortedSet<Long> = TreeSet(),
        private val rowValidator: (Long) -> Boolean =
            { pk -> pk > -1 && pk < rowMax.get() && !rowRecycler.contains(pk) },
        private val indexer: () -> Iterable<Long> = { buildIndex(rowMax.get(), rowRecycler) },
        private val retriever: (Long) -> Tuple<Long>? =
            { pk ->
                if (rowValidator(pk))
                    HashMapTuple(pk, properties).apply {
                        val rowVector: Matrix = matrix.selectRows(Ret.LINK, pk)
                        properties.mapIndexed { i, propertyType ->
                            set(propertyType,
                                rowVector.getNumericOrEnum(colValueTypes[i], 0 /* vector */, i.toLong()))
                        }
                    }
                else null
            },
    ): Table<Long>(
        counter = { matrix.rowCount },
        printer = { // matrix::toString,
            indexer()
                .map { rowVectorToString(it, matrix.selectRows(Ret.LINK, it), colValueTypes) }
                .joinToString { "\n" }
        },
        cleaner = matrix::clear,
        indexer = indexer,
        inserter = {
            if (rowRecycler.isEmpty()) {
                rowMax.getAndIncrement()
            } else {
                rowRecycler.first().apply { rowRecycler.remove(this) }
            }.let { row ->
                matrix.setRowLabel(row, rowLabeler(row))
                HashMapTuple(row, properties)
            }
        },
        retriever = retriever,
        remover = { row ->
            if (!rowValidator(row))
                error("Key $row not in [0,$rowMax], or already removed")
            val old = retriever(row)
            matrix.selectRows(Ret.LINK, row).clear()
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
                    error("Not enough columns ($columnCount)" + " to fit all properties (${properties.size})")
                properties.forEachIndexed { i, property ->
                    val col = i.toLong()
                    if(getColumnLabel(col) == null)
                        setColumnLabel(col, property.javaClass.simpleName)
                }
            }
        }

        companion object {

            fun rowVectorToString(rowNr: Long, rowVector: Matrix, columnValueTypes: List<Class<*>>) = buildString {
                append("#")
                append(rowNr)
                append(columnValueTypes.mapIndexed { i, valueType ->
                    "%6s".format(rowVector.getNumericOrEnum(valueType, 0 /* vector */, i.toLong())
                        ?.toString()?.substring(0, 6.coerceAtMost(length) - 1)
                        ?: "")
                }.joinToString(";"))
                append("]")
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
                val result = (0 until ignore.size)
                    .map { i -> Pair(if (i == 0) initialCutOff else ignore[i - 1] + 1, ignore[i]) }
                    .filter { (l, r) -> l != r } // skip empty sequences
                    .flatMap { (l, r) -> (l..r) }
                return result
            }
        }
    }

    @Test
    fun `test HashMapTable`() {
        val table: Table<Long> = HashMapTable(listOf(Prop1::class.java), AtomicLong()::getAndIncrement)
        table.changes.subscribe( { println("Change: $it") }, { println("Error: ${it.message}"); it.printStackTrace() } )
        table.insert(Prop1(11L))
        table.insert(listOf(Prop1(22L)))
        table.upsert(1L, Prop1(33L))
        table.delete(0L)
        assertNull(table.select(0L))
        println("size: ${table.size}, contents: $table")
    }

    @Test
    fun `test MatrixTable`() {
        val table: Table<Long> = MatrixTable(listOf(Prop1::class.java), buildMatrix {
            size = 10 by 1
            valueType = ValueType.OBJECT
        })
        table.changes.subscribe( { println("Change: $it") }, { println("Error: ${it.message}"); it.printStackTrace() } )
        table.insert(Prop1(11L))
        table.insert(listOf(Prop1(22L)))
        table.upsert(1L, Prop1(33L))
        table.delete(0L)
        assertNull(table.select(0L))
        table.insert(listOf(Prop1(44L)))
        table.insert(listOf(Prop1(55L)))
        table.delete(0L)
        table.delete(2L)
        println("size: ${table.size}, contents: $table")
    }

}