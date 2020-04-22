package io.smetweb.sim.cli

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.AdviceMode
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
open class DemoApplication {

    companion object {
        fun main(args: Array<String>) {
            runApplication<DemoApplication>(*args)
            // SpringApplication.run(DemoApplication::class.java, *args)
        }
    }
}