package io.smetweb.web

import io.smetweb.log.getLogger
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class VerticleTest {

    private val log = getLogger()

    @Test
    fun start_server() {
        val vertx: Vertx = Vertx.vertx()
        log.trace("Creating HTTP server")
        vertx.createHttpServer()
                .requestHandler { req -> req.response().end("Ok") }
                .listen(16969)
    }
}
