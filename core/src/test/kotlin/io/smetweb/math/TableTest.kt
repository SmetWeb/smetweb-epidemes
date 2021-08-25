package io.smetweb.math

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.ujmp.core.enums.ValueType
import java.util.concurrent.atomic.AtomicLong

class TableTest {

    class Prop1(value: Long): Table.Property<Long>(value)

    @Test
    fun `test HashMapTable`() {
        val table: Table<Long> = HashMapTable(listOf(Prop1::class.java), AtomicLong()::getAndIncrement)
        table.changes.subscribe( { println("Change: $it") }, { println("Error: ${it.message}"); it.printStackTrace() } )
        table.insert(Prop1(11L))
        assertEquals(11L, table.select(0L)!![Prop1::class.java]) { "Table contents: $table" }

        table.insert(listOf(Prop1(22L)))

        table.upsert(1L, Prop1(33L))
        val value1 = table.select(1L)
        assertEquals(33L, value1!![Prop1::class.java]) { "Table contents: $table" }

        table.delete(0L)
        val value0 = table.select(0L)
        assertNull(value0) { "Table contents: $table" }

        table.insert(listOf(Prop1(44L)))
        table.insert(listOf(Prop1(55L)))
        table.insert(listOf(Prop1(66L)))
        table.delete(0L)
        table.delete(2L)
        println("size: ${table.size}, contents: $table")
    }

    @Test
    fun `test MatrixTable`() {
        val table: Table<Long> = MatrixTable(listOf(Prop1::class.java), buildSparseObjectMatrix2D {
            size = 4 by 1
            valueType = ValueType.OBJECT
        })
        table.changes.subscribe( { println("Change: $it") }, { println("Error: ${it.message}"); it.printStackTrace() } )
        table.insert(Prop1(11L))
        assertEquals(11L, table.select(0L)!![Prop1::class.java]) { "Table contents: $table" }

        table.insert(listOf(Prop1(22L)))
        table.upsert(1L, Prop1(33L))
        val value1 = table.select(1L)
        assertEquals(33L, value1!![Prop1::class.java]) { "Table contents: $table" }

        table.delete(0L)
        val value0 = table.select(0L)
        assertNull(value0) { "Table contents: $table" }

        table.insert(listOf(Prop1(44L)))
        table.insert(listOf(Prop1(55L)))
        table.insert(listOf(Prop1(66L)))
        table.delete(0L)
        table.delete(2L)
        println("size: ${table.size}, contents: $table")
    }

}