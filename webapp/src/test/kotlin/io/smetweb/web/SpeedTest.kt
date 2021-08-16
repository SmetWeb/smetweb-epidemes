package io.smetweb.web

import io.reactivex.rxjava3.kotlin.toFlowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toFlux
import java.math.BigDecimal
import java.util.concurrent.ForkJoinPool
import java.util.stream.StreamSupport
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class SpeedTest {

@OptIn(ExperimentalTime::class)
@Test
fun `test parallelism`() {
    val mapperFlow: suspend (Long) -> BigDecimal = { it.toDouble().toBigDecimal().stripTrailingZeros() }
    val mapperNoSuspending: (Long) -> BigDecimal = { it.toDouble().toBigDecimal().stripTrailingZeros() }
    var sumFor: BigDecimal

    println("    For |         Sequence |      Async/await |    Flow/Suspend |   Single Stream | Parallel Stream |        RxKotlin |         WebFlux | tasks")
    (0..8).forEach { i ->
        val n = 10.0.pow(i.toDouble()).toLong()
        val iterable = (1..n)

        val elapsedFor: Duration = measureTime {
            sumFor = BigDecimal.ZERO
            for(j in iterable)
                sumFor = sumFor.add(mapperNoSuspending(j))
        }

        val elapsedSeq: Duration = if(n > 10000000) // avoid java.lang.OutOfMemoryError: Java heap space
            Duration.INFINITE
        else measureTime {
            val sum = iterable
                .map(mapperNoSuspending)
                .fold(BigDecimal.ZERO, BigDecimal::add)
            Assertions.assertEquals(sum, sumFor, "results should match")
        }

        val elapsedAsync: Duration = if(n > 1000000) // avoid java.lang.OutOfMemoryError: Java heap space
            Duration.INFINITE
        else measureTime {
            runBlocking(Dispatchers.IO) {
                val sum = iterable
                    .map { async { mapperFlow(it) } } // parallelize
                    .fold(BigDecimal.ZERO) { acc, bigDecimal -> acc.add(bigDecimal.await()) }
                Assertions.assertEquals(sum, sumFor, "results should match")
            }
        }

        val elapsedFlow: Duration = measureTime {
            runBlocking {
                val sum = iterable.asFlow()
                    .flowOn(Dispatchers.IO) // parallelize
                    .map(mapperFlow)
                    .fold(BigDecimal.ZERO, BigDecimal::add)
                Assertions.assertEquals(sum, sumFor, "results should match")
            }
        }

        val elapsedStream: Duration = measureTime {
            val sum = StreamSupport.stream(iterable.spliterator(), false)
                .map(mapperNoSuspending)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO)
            Assertions.assertEquals(sum, sumFor, "results should match")
        }

        val elapsedStreamPar: Duration = measureTime {
            val sum = ForkJoinPool().submit<BigDecimal> { // custom thread pool
                StreamSupport.stream(iterable.spliterator(), true) // parallelize
                    .map(mapperNoSuspending)
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO)
            }.join()
            Assertions.assertEquals(sum, sumFor, "results should match")
        }

        val elapsedRx: Duration = measureTime {
            val sum = iterable.toFlowable()
                .map(mapperNoSuspending)
                .reduce(BigDecimal::add)
                .blockingGet(BigDecimal.ZERO)
            Assertions.assertEquals(sum, sumFor, "results should match")
        }

        val elapsedFlux: Duration = measureTime {
            val sum = iterable.toFlux()
                .map(mapperNoSuspending)
                .reduce(BigDecimal::add)
                .blockOptional()
                .orElse(BigDecimal.ZERO)
            Assertions.assertEquals(sum, sumFor, "results should match")
        }

        val factorSeq = "%02.1f".format(elapsedFor / elapsedSeq)
        val factorAsync = "%02.1f".format(elapsedFor / elapsedAsync)
        val factorFlow = "%02.1f".format(elapsedFor / elapsedFlow)
        val factorStream = "%02.1f".format(elapsedFor / elapsedStream)
        val factorStreamPar = "%02.1f".format(elapsedFor / elapsedStreamPar)
        val factorRx = "%02.1f".format(elapsedFor / elapsedRx)
        val factorFlux = "%02.1f".format(elapsedFor / elapsedFlux)
        println("%7s".format(elapsedFor) +
                " | ${"%8s".format(elapsedSeq)} =${"%5s".format(factorSeq)}x" +
                " | ${"%8s".format(elapsedAsync)} =${"%5s".format(factorAsync)}x" +
                " | ${"%7s".format(elapsedFlow)} =${"%5s".format(factorFlow)}x" +
                " | ${"%7s".format(elapsedStream)} =${"%5s".format(factorStream)}x" +
                " | ${"%7s".format(elapsedStreamPar)} =${"%5s".format(factorStreamPar)}x" +
                " | ${"%7s".format(elapsedRx)} =${"%5s".format(factorRx)}x" +
                " | ${"%7s".format(elapsedFlux)} =${"%5s".format(factorFlux)}x" +
                " | 10^$i = $n")
    }
}

}