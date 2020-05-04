package io.smetweb.random

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PseudoRandomTest {

	@Test
	fun `seed consistency` () {
		assertTrue(PseudoRandomJava(1).nextBoolean())
		assertFalse(PseudoRandomKotlin(1).nextBoolean())
	}

}
