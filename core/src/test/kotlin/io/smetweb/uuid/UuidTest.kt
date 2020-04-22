package io.smetweb.uuid

import io.smetweb.log.getLogger
import io.smetweb.log.lazyString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UuidTest {

	private val log = getLogger()

	@Test
	fun `test uuid gen`() {
		val id1 = generateUUID()
		val id2 = generateUUID()

		Assertions.assertTrue(id2 > id1) { "expected: $id2 > $id1" }
		log.info("{} ({}}) > {}} ({}}) !", id2, lazyString(id2::created), id1, lazyString(id1::created))
	}
}