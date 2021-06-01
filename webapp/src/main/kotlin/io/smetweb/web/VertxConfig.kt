package io.smetweb.web

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VertxConfig {

    @Bean
    fun vertx(): Vertx =
            Vertx.vertx()

    @Bean
    fun rxVertx(vertx: Vertx): io.vertx.reactivex.core.Vertx =
            io.vertx.reactivex.core.Vertx(vertx)

    @Bean
    fun eventBus(vertx: Vertx): EventBus =
            vertx.eventBus()

    @Bean
    fun rxEventBus(rxVertx: io.vertx.reactivex.core.Vertx): io.vertx.reactivex.core.eventbus.EventBus =
            rxVertx.eventBus()

}