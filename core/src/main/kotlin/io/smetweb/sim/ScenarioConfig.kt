package io.smetweb.sim

import io.smetweb.time.parseTimeQuantity
import io.smetweb.time.parseZonedDateTime
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import java.time.*
import javax.measure.Quantity
import javax.measure.quantity.Time

@ConstructorBinding
@ConfigurationProperties(prefix = "scenario")
data class ScenarioConfig(

        @DefaultValue("default replication")
        val setupName: String,

        @DefaultValue("P6M")
        private val durationPeriod: String = "P1Y",

        @DefaultValue("2000-01-01")
        private val offsetDate: String = "2100-01-01",

        private val randomSeed: Long = System.currentTimeMillis(),

        @DefaultValue("default analyst")
        val analyst: String? = null,

        @DefaultValue("default replication")
        val description: String? = null

) {

        val start: ZonedDateTime =
                this.offsetDate.parseZonedDateTime()

        val epoch: Instant =
                this.start.toInstant()

        val duration: Quantity<Time> =
                this.durationPeriod.parseTimeQuantity(this.epoch)

        override fun toString(): String =
                "scenario[name: $setupName, start: $start, duration: $duration]"
}