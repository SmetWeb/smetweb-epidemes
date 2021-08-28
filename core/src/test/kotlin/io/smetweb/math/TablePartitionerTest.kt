package io.smetweb.math

import io.smetweb.log.getLogger
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.streams.toList

class TablePartitionerTest {

    private val log = getLogger()

    class Prop1(value: BigDecimal): Table.Property<BigDecimal>(value)

    @Test
    fun `test index partitions`() {
        val table: Table<Long> = MatrixTable(listOf(Prop1::class.java), buildSparseObjectMatrix2D { size = 4 by 1 })
        val partition = TablePartitioner(table) { e -> log.warn("Observed strata error: {}", e.message, e) }
            .stratify(Prop1::class.java, listOf(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN))

        table.changes.subscribe(
            { change -> log.info("Change {}\n\tpartition index: {}, strata: {}",
                change, partition.index().toList(), partition) },
            { e -> log.warn("Table error: {}", e.message, e) })

        table.insert(Prop1(1.opposite()))

        table.insert(Prop1(2.toDecimal()))
        table.upsert(1L, Prop1(3.toDecimal()))
        table.delete(0L)
        table.insert(Prop1(4.toDecimal()))
        table.insert(Prop1(5.opposite()))
        table.insert(Prop1(6.opposite()))
        table.delete(0L)
        table.delete(2L)

        println("size: ${table.size}, contents: $table")

    }

}