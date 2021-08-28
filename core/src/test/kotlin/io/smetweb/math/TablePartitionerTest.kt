package io.smetweb.math

import io.smetweb.log.getLogger
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TablePartitionerTest {

    private val log = getLogger()

    class Prop1(value: BigDecimal): Table.Property<BigDecimal>(value)

    @Test
    fun `test index partitions`() {
        val table: Table<Long> = MatrixTable(listOf(Prop1::class.java), buildSparseObjectMatrix2D { size = 4 by 1 })
        table.changes.subscribe(
            { change -> log.info("Observed change: {}", change) },
            { e -> log.info("Observed error: {}", e.message, e) })

        val partition = TablePartitioner(table) { e -> log.info("Observed error: {}", e.message, e) }
            .stratify(Prop1::class.java, listOf(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN))

        table.insert(Prop1(11.opposite()))

        table.insert(listOf(Prop1(22.toDecimal())))
        table.upsert(1L, Prop1(33.opposite()))
        table.delete(0L)
        table.insert(listOf(Prop1(44.toDecimal())))
        table.insert(listOf(Prop1(55.opposite())))
        table.insert(listOf(Prop1(66.toDecimal())))
        table.delete(0L)
        table.delete(2L)

        println("size: ${table.size}, contents: $table, partition: $partition")

    }

}