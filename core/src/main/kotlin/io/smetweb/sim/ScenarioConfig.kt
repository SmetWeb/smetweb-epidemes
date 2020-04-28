package io.smetweb.sim

import io.smetweb.time.parseDuration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

        @DefaultValue("default analyst")
        val analyst: String? = null,

        @DefaultValue("default replication")
        val description: String? = null

) {

        val zone: ZoneId = ZoneId.systemDefault()

        val epoch: Instant by lazy {
                LocalDate.parse(this.offsetDate).atStartOfDay().atZone(this.zone).toInstant()
        }

        val duration: Quantity<Time> by lazy {
                this.durationPeriod.parseDuration(this.epoch)
        }

        override fun toString(): String =
                "scenario[name: $setupName, start: ${epoch.atZone(zone)}, duration: $duration]"
}