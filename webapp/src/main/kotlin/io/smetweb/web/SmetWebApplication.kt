package io.smetweb.web

import io.smetweb.log.getLogger
import io.smetweb.sim.ScenarioConfig
import io.vertx.core.Verticle
import io.vertx.reactivex.core.RxHelper
import io.vertx.reactivex.core.Vertx
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.AdviceMode
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.reactive.config.EnableWebFlux
import javax.annotation.PostConstruct

// TODO: replace Spring-boot by Quarkus (faster due to compile-time indexing)?
@SpringBootApplication(scanBasePackages = ["io.smetweb"])
@EnableConfigurationProperties(ScenarioConfig::class, SmetWebConfig::class)
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@EnableScheduling
@EnableWebFlux
// JPA
@EntityScan(basePackages = ["io.smetweb.uuid", "io.smetweb.time", "io.smetweb.sim.event"])
class SmetWebApplication {

    @Autowired
    private lateinit var rxVertx: Vertx

    @Autowired
    private lateinit var verticles: List<Verticle>

    private val log = getLogger()

    @EventListener
    fun onApplicationReady(e: ApplicationReadyEvent) {
        log.info("Application started and ready for requests.")
    }

    @EventListener
    fun onContextClosed(e: ContextClosedEvent) {
        log.info("Application context closed, no longer processing requests.")
    }

    // application context must load before Vertx can start deploying
    @PostConstruct
    fun deployVerticle() {
        this.verticles.forEach { verticle ->
            // TODO get deployment options from verticle via common interface?
            log.debug("Deploying verticle {}", verticle::class.java)
            RxHelper.deployVerticle(rxVertx, verticle).subscribe( {
                log.info("Deployed verticle {}: {}", verticle::class.java, it)
            } ) { e ->
                log.error("Failed to deploy verticle {}: {}", verticle::class.java, e.message, e)
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // spring-boot
            SpringApplication.run(SmetWebApplication::class.java, *args)
        }
    }
}