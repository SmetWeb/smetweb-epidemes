package io.smetweb.webapp.demo

import io.smetweb.log.getLogger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.AdviceMode
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@EnableSwagger2
class DemoWebApplication : SpringBootServletInitializer() {

    private val log = getLogger()

    init {
        setRegisterErrorPageFilter(false)
    }

    @EventListener
    fun onApplicationReady(e: ApplicationReadyEvent) {
        log.info("Application started and ready for requests.")
    }

    @EventListener
    fun onContextClosed(e: ContextClosedEvent) {
        log.info("Application context closed, no longer processing requests.")
    }

    override fun configure(builder: SpringApplicationBuilder): SpringApplicationBuilder {
        return builder.sources(DemoWebApplication::class.java)
    }

    companion object {
        fun main(args: Array<String>) {
            runApplication<DemoWebApplication>(*args)
        }
    }
}