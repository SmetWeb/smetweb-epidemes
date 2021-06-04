package io.smetweb.sim.dsol

import com.fasterxml.jackson.databind.ObjectMapper
import io.smetweb.math.NUMBER_SYSTEM
import io.smetweb.time.MICROSECOND
import io.smetweb.time.MILLISECOND
import io.smetweb.time.NANOSECOND
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tech.units.indriya.function.Calculus
import tech.units.indriya.unit.Units
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class DsolTimeRefTest {

	@Test
	fun `parsing and conversion` () {
		assertEquals(NUMBER_SYSTEM, Calculus.currentNumberSystem())
		assertTrue(DsolTimeRef("1 ns").ge0())
		assertTrue(DsolTimeRef("1E3 ms").eq(DsolTimeRef(1)), "value should be equivalent")
		assertNotEquals(DsolTimeRef("1.1E9 ns"), DsolTimeRef(1.1), "units should be different")
		assertFalse(DsolTimeRef(".0000000000000000000005 ns").le0(), ".0000000000000000000005 ns is not less than or equal to zero")
		assertTrue(DsolTimeRef(0.00000000, MICROSECOND).ge0(), "0.00000000 ms is greater than or equal to absolute zero")
		val min1 = DsolTimeRef("1 min")
		val sec1 = DsolTimeRef(60)
		val sec2 = sec1.get()
		// seconds to minutes converter: Rational(x -> x * 0.01666666666666666666666666666666667), see ScaleHelper::convertTo @ quantity.getUnit().getConverterTo(anotherUnit)
		assertEquals(0, min1.compareTo(sec1), "1 minute ($min1) compares equal to 60 seconds ($sec1 or $sec2) as per converter: ${sec2.unit.getConverterTo(Units.MINUTE).inverse()}")
		assertTrue(min1.eq(sec1), "1 minute compares equal to 60 seconds")
		assertEquals(BigDecimal::class.java, DsolTimeRef(1.1).get().value::class.java)
		assertEquals(BigDecimal::class.java, DsolTimeRef("1.1E9 ns").get().value::class.java)
	}

	@Test
	fun `hashcode equivalence` () {
		assertEquals(DsolTimeRef("1.1E9 ns").hashCode(), DsolTimeRef(1.1).hashCode(), "hash codes should be same")
		assertNotEquals(DsolTimeRef("1.10001E9 ns").hashCode(), DsolTimeRef(1.1).hashCode(), "hash codes should differ")
	}

	@Test
	fun `timestamp conversion` () {

		val now = Instant.now()
		val later = now.atOffset(ZoneOffset.UTC).plusDays(2).plusHours(12)
		val per1 = DsolTimeRef(2.5, Units.DAY)
		val per2 = DsolTimeRef(Duration.between(now, later))
		// seconds to days converter: Rational(x -> x * 0.00001157407407407407407407407407407407), see ScaleHelper::convertTo @ quantity.getUnit().getConverterTo(anotherUnit)
		assertEquals(0, per2.compareTo(per1), "2.5 days ($per1) compares equal to 2 days and 12 hours ($per2 or ${per2.get().to(Units.DAY)} as per converter: ${per1.get().unit.getConverterTo(Units.DAY).inverse()})")
		assertEquals(0, per1.compareTo(per2), "2.5 days ($per1) compares equal to 2 days and 12 hours ($per2 or ${per2.get().to(Units.DAY)} as per converter: ${per2.get().unit.getConverterTo(Units.DAY)})")

		assertEquals(now, DsolTimeRef(0).toInstant(now))
		assertEquals(now.plusNanos(4), DsolTimeRef(0.000004, MILLISECOND).toInstant(now))
		assertEquals(now, DsolTimeRef("-0").toInstant(now))
		assertEquals(now, DsolTimeRef("0E9").toInstant(now))
		assertEquals(now, DsolTimeRef("0 ms").toInstant(now))
		assertEquals(now.plusNanos(10).plusSeconds(30),
				DsolTimeRef(10, NANOSECOND)
						.apply{ add(BigDecimal.valueOf(30)) }
						.toInstant(now))
	}

	@Test
	fun `string conversion` () {
		val t = DsolTimeRef(0.0000004, MILLISECOND)
		assertEquals(t, DsolTimeRef(t.toString()))

		val om = ObjectMapper()
		assertEquals(t, om.readValue(om.writeValueAsString(t), DsolTimeRef::class.java))
	}
}
