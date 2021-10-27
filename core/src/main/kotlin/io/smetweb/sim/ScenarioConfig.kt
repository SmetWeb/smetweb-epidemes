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

	@DefaultValue("default setup")
	val setupName: String,

	@DefaultValue("P6M")
	private val durationPeriod: String = "P6M",

	@DefaultValue("2000-01-01")
	private val offsetDate: String = "2000-01-01",

	val randomSeed: Long = System.currentTimeMillis(),

	@DefaultValue("default analyst")
	val analyst: String? = null,

	@DefaultValue("default replication")
	val description: String? = null

) {

	val defaultZone: ZoneId =
		ZoneId.systemDefault()

	val start: ZonedDateTime =
		this.offsetDate.parseZonedDateTime(defaultZone)

	val epoch: Instant =
		this.start.toInstant()

	val duration: Quantity<Time> =
		this.durationPeriod.parseTimeQuantity(offset = this.epoch)

	override fun toString(): String =
		"${ScenarioConfig::class.java.simpleName}[name: $setupName, seed: $randomSeed, " +
				"start + length: $offsetDate + $durationPeriod (i.e. $start + $duration)]"
}