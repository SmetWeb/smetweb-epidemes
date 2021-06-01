package io.smetweb.time

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tech.units.indriya.unit.Units
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.*
import java.time.temporal.UnsupportedTemporalTypeException
import java.util.*
import java.util.concurrent.TimeUnit

class TimeRefTest {

	@Test
	fun `factory methods`() {
		val now = Instant.now()
		val nowDate = Date.from(now)
		val epoch = Instant.EPOCH
		val epochDate = Date.from(epoch)
		val delta = Duration.between(epoch, now)
		assertEquals(delta, TimeRef.of(delta).toDuration(), "conversion from/to Duration")
		assertEquals(now, TimeRef.of(now, epoch).toInstant(epoch), "conversion from/to Instant")
		assertEquals(nowDate, TimeRef.of(nowDate, epochDate).toDate(epochDate), "conversion from/to Date")
	}

	@Test
	fun `time unit conversion`() {
		// happy
		for(unit in TimeUnit.values())
			assertEquals(unit, TimeRef.resolveTimeUnit(TimeRef.resolveUnit(unit)))

		// unhappy
		assertThrows(DateTimeException::class.java) { TimeRef.resolveTimeUnit(Units.WEEK) }
	}

	@Test
	fun `temporal unit conversion`() {
		// happy
		for(unit in listOf(NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, YEARS))
			assertEquals(unit, TimeRef.resolveTemporalUnit(TimeRef.resolveUnit(unit)))

		// unhappy (multitudes of [ambiguous] year are currently undefined)
		for(unit in listOf(MONTHS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER))
			assertThrows(UnsupportedTemporalTypeException::class.java) { TimeRef.resolveTemporalUnit(TimeRef.resolveUnit(unit)) }
	}

	@Test
	fun `duration conversion`() {
		// happy
		for(unit in listOf(NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS))
			assertEquals(Duration.ZERO, TimeRef.T_ZERO.toDuration(unit))

		// unhappy (anything larger than a day is undefined/imprecise, see ChronoUnit#isDurationEstimated)
		for(unit in listOf(WEEKS, YEARS))
			assertThrows(UnsupportedTemporalTypeException::class.java) { TimeRef.T_ZERO.toDuration(unit) }

		// unhappy (multitudes of [ambiguous] year are currently undefined)
		for(unit in listOf(MONTHS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER))
			assertThrows(UnsupportedTemporalTypeException::class.java) { TimeRef.T_ZERO.toDuration(unit) }
	}

	@Test
	fun `ordinal comparison`() {
//		assertTrue(0 > Tick.Ordinal.of)
	}
}