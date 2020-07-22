package io.smetweb.interact.zmq

import io.smetweb.sim.dsol.REPORT_INTERVAL
import org.awaitility.kotlin.await
import org.djutils.logger.CategoryLogger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.zeromq.SocketType
import org.zeromq.ZMQ
import java.time.Duration

class ZeromqTest {

    private lateinit var context: ZMQ.Context
    private lateinit var responder: ZMQ.Socket

    @BeforeAll
    fun `setup zmq server`() {
        context = ZMQ.context(1)


        // Socket to talk to clients
        responder = context.socket(SocketType.REP)
        responder.bind("tcp://*:5555")
        while (!Thread.currentThread().isInterrupted) {
            // Wait for next request from the client
            val request = responder.recv(0)
            println("Received $request")

            // Do some 'work'
            try {
                Thread.sleep(1000)
            } catch (exception: InterruptedException) {
                CategoryLogger.always().error(exception)
            }

            // Send reply back to client
            val reply = "World"
            responder.send(reply.toByteArray(), 0)
        }
    }

    @Test
    fun `client receives response to query`() {
        val dt = Duration.ofMillis(1000)
        // will poll every 100ms, and kill the test on first success, or times out (failure)
        await.atMost(dt).untilAsserted {

        }

    }

    @AfterAll
    fun `close server`() {
        context.term()
        responder.close()
    }
}