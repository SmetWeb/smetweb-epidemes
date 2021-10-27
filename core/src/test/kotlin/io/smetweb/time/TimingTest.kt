package io.smetweb.time

import io.smetweb.log.getLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.units.indriya.unit.Units

class TimingTest {

	private val log = getLogger()

	@Test
	fun `parsing variants`() {
		val now = 1
		val dt = 4
		val n = 3

		assertThrows(RuntimeException::class.java) { "that's just wrong dude".parseTiming() }

		// testing with base (default) time units, i.e. TimeRef.BASE_UNIT
		val parseJSR = "$dt ".parseTiming(max = n).iterate(TimeRef.of(now)).toList()
		assertEquals((0 until n).map { TimeRef.of(now + it * dt) }, parseJSR, "parsing base units")

		val unitISO = Units.DAY // ISO-8601 duration: "P*D" (or "-P-1D" as also allowed by Joda): daily intervals
		val parseISO = "-P-${dt}D".parseTiming(max = n).iterate(TimeRef.of(now, unitISO)).toList()
		assertEquals((0 until n).map { TimeRef.of(now + it * dt, unitISO) }, parseISO) { "parsing unit: $unitISO" }

		val unitCRON = Units.MINUTE // CRON: "0 /4 * * * ?": zeroth second of each 4th minute of each hour, date, month, week (year)
		val parseCRON = "0 /${dt} * * * ?".parseTiming(max = n).iterate(TimeRef.of(now, unitCRON)).toList()
		assertEquals((0 until n).map { TimeRef.of(now + it * dt, unitCRON) }, parseCRON, "parsing unit: $unitCRON")

//		val unitICAL = Units.DAY // iCal:
//		val parseICAL = "FREQ=DAILY;COUNT=$n;INTERVAL=$dt"
//			.parseTiming(max = n).iterate(TimeRef.of(now, unitICAL)).toList()
//		assertEquals((0 until n).map { TimeRef.of(now + it * dt, unitICAL) }, parseICAL, "parsing unit: $unitICAL")
	}
}