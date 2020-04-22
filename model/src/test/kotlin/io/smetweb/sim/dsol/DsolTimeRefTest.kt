package io.smetweb.sim.dsol

import com.fasterxml.jackson.databind.ObjectMapper
import io.smetweb.time.TimeRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tec.uom.se.unit.Units
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class DsolTimeRefTest {

	@Test
	fun `parsing and conversion` () {
		assertTrue(DsolTimeRef("1 ns").ge0())
		assertTrue(DsolTimeRef("1E3 ms").eq(DsolTimeRef(1)), "value should be equivalent")
		assertNotEquals(DsolTimeRef("1.1E9 ns"), DsolTimeRef(1.1), "units should be different")
		assertFalse(DsolTimeRef(".0000000000000000000005 ns").le0())
		assertTrue(DsolTimeRef(0.00000000, TimeRef.MICROSECOND).ge0())
		assertTrue(DsolTimeRef("1 min").eq(DsolTimeRef(60)))
		assertTrue(DsolTimeRef(12).get().value is BigDecimal)
	}

	@Test
	fun `hashcode equivalence` () {
		assertEquals(DsolTimeRef("1.1E9 ns").hashCode(), DsolTimeRef(1.1).hashCode(), "hash codes should be same")
		assertNotEquals(DsolTimeRef("1.10001E9 ns").hashCode(), DsolTimeRef(1.1).hashCode(), "hash codes should be same")
	}

	@Test
	fun `timestamp conversion` () {

		val now = Instant.now()
		assertTrue(DsolTimeRef(2.5, Units.DAY).eq(
				DsolTimeRef(Duration.between(now, now.atOffset(ZoneOffset.UTC)
						.plusDays(2)
						.plusHours(12)))))

		assertEquals(now, DsolTimeRef(0).toInstant(now))
		assertEquals(now.plusNanos(4), DsolTimeRef(0.000004, TimeRef.MILLISECOND).toInstant(now))
		assertEquals(now, DsolTimeRef("-0").toInstant(now))
		assertEquals(now, DsolTimeRef("0E9").toInstant(now))
		assertEquals(now, DsolTimeRef("0 ms").toInstant(now))
		assertEquals(now.plusNanos(10).plusSeconds(30),
				DsolTimeRef(10, TimeRef.NANOSECOND)
						.apply{ add(BigDecimal.valueOf(30)) }
						.toInstant(now))
	}

	@Test
	fun `string conversion` () {
		val t = DsolTimeRef(0.0000004, TimeRef.MILLISECOND)
		assertEquals(t, DsolTimeRef(t.toString()))

		val om = ObjectMapper()
		assertEquals(t, om.readValue(om.writeValueAsString(t), DsolTimeRef::class.java))
	}
}