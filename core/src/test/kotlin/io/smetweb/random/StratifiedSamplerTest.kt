package io.smetweb.random

import io.smetweb.log.getLogger
import io.smetweb.math.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class StratifiedSamplerTest {

    private val log = getLogger()

    class Prop1(value: BigDecimal): Table.Property<BigDecimal>(value)

    @Test
    fun `test stratified sampling`() {
        val table: Table<Long> = MatrixTable(listOf(Prop1::class.java), buildSparseObjectMatrix2D { size = 4 by 1 })
//        val partition = TablePartitioner(table) { e -> log.warn("Observed strata error: {}", e.message, e) }
//            .stratify(Prop1::class.java, listOf(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN))
        val rng = PseudoRandomEcj(1234)
        val sampler = StratifiedSampler.of(table, rng)
            .splitBy(Prop1::class.java, BigDecimal.ONE, BigDecimal.TEN)

        table.changes.subscribe(
            { change -> log.info("Change {}\n\tstrata: {}", change, sampler.partitioner) },
            { e -> log.warn("Table error: {}", e.message, e) })

        table.insert(Prop1(1.opposite())) // key: 0
        table.insert(Prop1(2.toDecimal())) // key: 1
        table.upsert(1L, Prop1(3.toDecimal())) // key: 1
        table.delete(0L)
        table.insert(Prop1(4.toDecimal())) // key: 0
        table.insert(Prop1(50.toDecimal())) // key: 2
        table.insert(Prop1(6.opposite())) // key: 3

        val binCounts = mutableMapOf<Range<*>, MutableList<Comparable<*>>>()
        (1..100)
            .map { sampler.match(BigDecimal.ONE)
                .draw { k, v, args ->
                    log.trace( "Sampler strata {} deviation: {} in {}", args, k.simpleName, v );
                    true
                }
            }
            .forEach { tuple ->
                table.properties.forEach { prop ->
                    val value = tuple[prop]!! as Comparable<*>
                    val bin: Range<*> = sampler.partitioner.strataFor(prop, value)[0].first
                    binCounts.getOrPut(bin) { mutableListOf() }.add(value)
                }
            }
        println("size: ${table.size}, contents: $table, samples: $binCounts \n ${binCounts.map { (k, v) -> Pair(k, v.size) }}")

    }

}