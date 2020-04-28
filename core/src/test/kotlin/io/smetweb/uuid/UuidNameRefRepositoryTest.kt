package io.smetweb.uuid

import io.smetweb.log.getLogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration

@EnableAutoConfiguration
@SpringBootTest
@ContextConfiguration(classes = [UuidNameRefRepository::class])
class UuidNameRefRepositoryTest {

	private val log = getLogger()

	@Autowired
	private lateinit var repository: UuidNameRefRepository

	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	@Test
	fun `UuidNameRefDao crud test`() {
		val nl = "\n\r"
		val nameRef1a = UuidNameRef("dep", UuidNameRef("org", UuidNameRef()))
		val nameRef1b = UuidNameRef("dep", UuidNameRef("org", UuidNameRef()))

		log.info("Started tests for '$nameRef1a' and '$nameRef1b'")

		// fetch, expect no match
		val result1a = repository.findByKey(nameRef1a)
		val result1b = repository.findByKey(nameRef1b)
		assertNull(result1a)
		assertNull(result1b)

		// store
		repository.save(nameRef1a)
		repository.save(nameRef1b)
		val result2 = getRows(UuidNameRefEntity.TABLE_NAME)
		log.info("Saved '$nameRef1a' and '$nameRef1b', db: ${result2.joinToString(nl)}")
		assertEquals(2 + 2 + 1, result2.size)

		// fetch, expect match
		val result3a = repository.findByKey(nameRef1a)
		val result3b = repository.findByKey(nameRef1b)
		assertNotNull(result3a)
		assertNotNull(result3b)

		// remove
		val result4a = repository.delete(nameRef1a)
		val result4b = repository.delete(nameRef1b)
		val result4 = getRows()
		log.info("Removed '$nameRef1a' and '$nameRef1b', db: ${result4.joinToString(nl)}")
		assertEquals(1, result4a)
		assertEquals(1, result4b)
		assertEquals(1 + 1 + 1, result4.size)

		// fetch, expect no match
		val result5a = repository.findByKey(nameRef1a)
		val result5b = repository.findByKey(nameRef1b)
		assertNull(result5a)
		assertNull(result5b)

		log.info("Completed tests for '$nameRef1a' and '$nameRef1b'")
	}

	private fun getRows(tableName: String = UuidNameRefEntity.TABLE_NAME): List<List<String>> =
			mutableListOf<List<String>>().let { result ->
				this.jdbcTemplate.query("SELECT * FROM $tableName") { rs ->
					val cols = 1 .. rs.metaData.columnCount
					result.add(cols.map { rs.metaData.getColumnName(it) })
					while(!rs.isAfterLast) {
						result.add(cols.map { rs.getString(it) })
						rs.next()
					}
				}
				result
			}
}