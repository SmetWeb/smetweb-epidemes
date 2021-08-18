package io.smetweb.math

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicLong

class Prop1(value: Long): Table.Property<Long>(value)

class TableTest {

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
        private val map: MutableMap<PK, Tuple<PK>> = mutableMapOf(),
    ): Table<PK>(
        counter = { map.size.toLong() },
        printer = map::toString,
        indexer = { map.keys },
        retriever = map::get,
        remover = { map.remove(it) },
        cleaner = map::clear,
        inserter = { HashMapTuple(keyGen(), properties).apply { map[this.key] = this } }
    )

    @Test
    fun `test interfaces`() {
        val table: Table<Long> = HashMapTable(listOf(Prop1::class.java), AtomicLong()::getAndIncrement)
        table.changes.subscribe( { println("Change: $it") }, { println("Error: ${it.message}"); it.printStackTrace() } )
        table.insert(Prop1(11L))
        table.insert(listOf(Prop1(22L)))
        table.upsert(1L, Prop1(33L))
        table.delete(0L)
        assertNull(table.select(0L))
        println("size: ${table.size}, contents: $table")
    }

}