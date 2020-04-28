package io.smetweb.sim.event

import io.smetweb.uuid.UuidNameRef
import io.smetweb.sim.dsol.DsolTimeRef
import io.smetweb.fact.ResultKind
import io.smetweb.log.getLogger
import io.smetweb.sim.ScenarioConfig
import io.smetweb.uuid.UuidNameRefRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@DataJpaTest
@EntityScan(basePackages = ["io.smetweb.uuid", "io.smetweb.time", "io.smetweb.sim.event"])
@ContextConfiguration(classes = [UuidFactRepository::class, UuidNameRefRepository::class])
@EnableConfigurationProperties(ScenarioConfig::class)
@EnableAutoConfiguration
@AutoConfigureTestDatabase // default is in-memory, otherwise set (replace=Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // default is rollback all, otherwise set
open class UuidFactEntityTest {

	private val log = getLogger()

	@Autowired
	private lateinit var uuidFactRepository: UuidFactRepository

	@Test
	fun `entity setup` () {
		val tick = DsolTimeRef.T_ZERO
		val root = UuidNameRef()
		val me = UuidNameRef("actor1", root)
		val them = UuidNameRef("actor2", root)
		val ctx = UuidResultExchange(resultKind = ResultKind.of(UuidNameRef("T01", root)),
				executorRef = me, initiatorRef = them)
		val fact = UuidFact(occur = tick, exchange = ctx)
		uuidFactRepository.save(fact)

		val fact2 = UuidFact(occur = tick, exchange = ctx, ordinal = fact.ordinal.next())
		assertNotEquals(fact2.id.get(), fact.id.get(), "generated new unique fact id")

		log.info(" Persisted {}", fact)
		log.info("Persisting {}", fact2)
		uuidFactRepository.save(fact2)
		assertNotEquals(fact2.ordinal, fact.ordinal, "generated new unique ctx ordinal")

		val created = uuidFactRepository.save(fact)
//		assertEquals(fact.getId(), created, "generated new private key")

		val removedCount = uuidFactRepository.delete(fact)
		assertEquals(1, removedCount, "just one record removed")

		val recreated = uuidFactRepository.save(fact)
		assertEquals(created, recreated, "persisted same values")
//		assertNotEquals(created.pk, recreated.pk, "generated new private key")

		val constraintViolation = uuidFactRepository.save(fact)
		assertEquals(recreated, constraintViolation, "unique constraint violation averted")

		// just one record removed
		val removedAgainCount = uuidFactRepository.delete(fact)
		assertEquals(1, removedAgainCount, "just one record removed again")

	}

}