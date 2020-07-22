package io.smetweb.web

import io.smetweb.log.getLogger
import io.smetweb.sim.ScenarioConfig
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.AdviceMode
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication(scanBasePackages = ["io.smetweb"])
@EntityScan(basePackages = ["io.smetweb.uuid", "io.smetweb.time", "io.smetweb.sim.event"])
@EnableConfigurationProperties(ScenarioConfig::class)
@EnableScheduling
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
class SmetWebApplication : SpringBootServletInitializer() {

    private val log = getLogger()

//    init {
//        setRegisterErrorPageFilter(false)
//    }

//    @Bean (workaround for springfox-data-rest, see https://github.com/springfox/springfox/issues/2932#issuecomment-578799229)
//    fun discoverers(relProviderPluginRegistry: OrderAwarePluginRegistry<LinkDiscoverer, MediaType>):
//            PluginRegistry<LinkDiscoverer, MediaType> = relProviderPluginRegistry

    @EventListener
    fun onApplicationReady(e: ApplicationReadyEvent) {
        log.info("Application started and ready for requests.")
    }

    @EventListener
    fun onContextClosed(e: ContextClosedEvent) {
        log.info("Application context closed, no longer processing requests.")
    }

    override fun configure(builder: SpringApplicationBuilder): SpringApplicationBuilder {
        return builder.sources(SmetWebApplication::class.java)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<SmetWebApplication>(*args) {
                setBannerMode(Banner.Mode.OFF)
            }
        }
    }
}