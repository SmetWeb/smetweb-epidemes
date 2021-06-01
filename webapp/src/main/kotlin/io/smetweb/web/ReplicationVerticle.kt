package io.smetweb.web

import io.smetweb.log.getLogger
import io.smetweb.sim.ScenarioConfig
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.reactivex.core.eventbus.EventBus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ReplicationVerticle(
        private val smetWebConfig: SmetWebConfig,
        private val scenarioConfig: ScenarioConfig,
        private val rxEventBus: EventBus
) : CoroutineVerticle() {

    private val log = getLogger()

    // from tutorial: https://www.baeldung.com/spring-vertx#1-sender-verticle
    override suspend fun start() {
        // obtain params
        // init/run replication on worker thread
        // dispatch result to event bus

        super.start()

        log.info("Loaded configs: \n\tsmetweb = {}\n\tscenario = {}", smetWebConfig, scenarioConfig)

        rxEventBus.consumer<String>("do.something").toObservable()
                .subscribe( { msg ->
                    log.info("Got message from {}: {}", msg.replyAddress() ?: "<unknown>", msg.body())
                    msg.reply("World")
                } ) { err ->
                    log.error("Unable to read: {}", err.message, err)
                }
    }

}