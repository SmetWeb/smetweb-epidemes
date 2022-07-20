package io.smetweb.xml

import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

class XmlUtilTest {

	@Test
	fun jodaParserTest() {
		val dt = DateTime()
		val res = dt.toXML().toDateTime()
		assertNotEquals(dt, res, "Chronology should not match due to zone-offset conversion")
		assertEquals(0, dt.compareTo(res), "AbstractIntant.compareTo should ignore Chronology mismatch")
		assertTrue(dt.isEqual(res), "AbstractIntant.isEqual should ignore Chronology mismatch")
	}

	@Test
	fun jsr310ParserTest() {
		val dur = Duration.parse("P1DT2H3M4.058S")
		val res = dur.toXML().toDuration()
	}

	@Test
	fun streamParserTest() {

	}
}